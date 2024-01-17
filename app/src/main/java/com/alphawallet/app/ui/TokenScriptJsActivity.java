package com.alphawallet.app.ui;

import static com.alphawallet.app.widget.AWalletAlertDialog.ERROR;
import static com.alphawallet.app.widget.AWalletAlertDialog.WARNING;
import static org.web3j.protocol.core.methods.request.Transaction.createFunctionCallTransaction;
import static java.util.Collections.singletonList;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.analytics.Analytics;
import com.alphawallet.app.entity.AnalyticsProperties;
import com.alphawallet.app.entity.GasEstimate;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.TransactionReturn;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.analytics.ActionSheetSource;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.util.ShortcutUtils;
import com.alphawallet.app.viewmodel.DappBrowserViewModel;
import com.alphawallet.app.viewmodel.TokenFunctionViewModel;
import com.alphawallet.app.web3.OnEthCallListener;
import com.alphawallet.app.web3.OnSignMessageListener;
import com.alphawallet.app.web3.OnSignPersonalMessageListener;
import com.alphawallet.app.web3.OnSignTransactionListener;
import com.alphawallet.app.web3.OnSignTypedMessageListener;
import com.alphawallet.app.web3.OnWalletActionListener;
import com.alphawallet.app.web3.OnWalletAddEthereumChainObjectListener;
import com.alphawallet.app.web3.Web3View;
import com.alphawallet.app.web3.entity.Address;
import com.alphawallet.app.web3.entity.WalletAddEthereumChainObject;
import com.alphawallet.app.web3.entity.Web3Call;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.ActionSheet;
import com.alphawallet.app.widget.ActionSheetDialog;
import com.alphawallet.app.widget.ActionSheetSignDialog;
import com.alphawallet.app.widget.CertifiedToolbarView;
import com.alphawallet.ethereum.EthereumNetworkBase;
import com.alphawallet.hardware.SignatureFromKey;
import com.alphawallet.token.entity.EthereumMessage;
import com.alphawallet.token.entity.EthereumTypedMessage;
import com.alphawallet.token.entity.SignMessageType;
import com.alphawallet.token.entity.Signable;
import com.alphawallet.token.entity.XMLDsigDescriptor;

import org.jetbrains.annotations.NotNull;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.utils.Numeric;

import java.math.BigInteger;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@AndroidEntryPoint
public class TokenScriptJsActivity extends BaseActivity implements StandardFunctionInterface, ActionSheetCallback,
        OnSignTransactionListener, OnSignPersonalMessageListener, OnSignTypedMessageListener, OnSignMessageListener,
        OnEthCallListener, OnWalletAddEthereumChainObjectListener, OnWalletActionListener
{
    private DappBrowserViewModel viewModel;
    private Token token;
    private BigInteger tokenId;
    private NFTAsset asset;
    private String sequenceId;
    private ActionSheet confirmationDialog;
    private AWalletAlertDialog dialog;
    private Animation rotation;
    private ActivityResultLauncher<Intent> handleTransactionSuccess;
    private ActivityResultLauncher<Intent> getGasSettings;
    private long chainId;
    private Web3View tokenScriptView;
    private Wallet wallet;
    private NetworkInfo activeNetwork;
    private AWalletAlertDialog chainSwapDialog;
    private AWalletAlertDialog resultDialog;
    private AWalletAlertDialog errorDialog;
    private AddEthereumChainPrompt addCustomChainDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tokenscript_js);

        initViews();

        toolbar();

        initIntents();

        initViewModel();
    }

    private void initIntents()
    {
        handleTransactionSuccess = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result ->
                {
                    if (result.getData() == null) return;
                    String transactionHash = result.getData().getStringExtra(C.EXTRA_TXHASH);
                    //process hash
                    if (!TextUtils.isEmpty(transactionHash))
                    {
                        Intent intent = new Intent();
                        intent.putExtra(C.EXTRA_TXHASH, transactionHash);
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                });

        getGasSettings = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> confirmationDialog.setCurrentGasIndex(result));
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (viewModel != null)
        {
            getIntentData();
        }
        else
        {
            recreate();
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        viewModel.onDestroy();
    }

    private void initViews()
    {
        rotation = AnimationUtils.loadAnimation(this, R.anim.rotate_refresh);
        rotation.setRepeatCount(Animation.INFINITE);
    }

    private void getIntentData()
    {
        chainId = getIntent().getLongExtra(C.EXTRA_CHAIN_ID, EthereumNetworkBase.MAINNET_ID);
        tokenId = new BigInteger(getIntent().getStringExtra(C.EXTRA_TOKEN_ID));
        asset = getIntent().getParcelableExtra(C.EXTRA_NFTASSET);
        sequenceId = getIntent().getStringExtra(C.EXTRA_STATE);
        String walletAddress = getWalletFromIntent();
        //viewModel.loadWallet(walletAddress);
        if (C.ACTION_TOKEN_SHORTCUT.equals(getIntent().getAction()))
        {
            handleShortCut(walletAddress);
        }
        else
        {
            token = resolveAssetToken();
            setup();
        }

        //viewModel.startGasPriceUpdate(chainId);
    }

    private String getWalletFromIntent()
    {
        Wallet w = getIntent().getParcelableExtra(C.Key.WALLET);
        if (w != null)
        {
            return w.address;
        }
        else
        {
            return getIntent().getStringExtra(C.Key.WALLET);
        }
    }

    private Token resolveAssetToken()
    {
        if (asset != null && asset.isAttestation())
        {
            return viewModel.getTokenService().getAttestation(chainId, getIntent().getStringExtra(C.EXTRA_ADDRESS), asset.getAttestationID());
        }
        else
        {
            return viewModel.getTokenService().getToken(chainId, getIntent().getStringExtra(C.EXTRA_ADDRESS));
        }
    }

    private void handleShortCut(String walletAddress)
    {
        String tokenAddress = getIntent().getStringExtra(C.EXTRA_ADDRESS);
        token = viewModel.getTokenService().getToken(walletAddress, chainId, tokenAddress);
        if (token == null)
        {
            ShortcutUtils.showConfirmationDialog(this, singletonList(tokenAddress), getString(R.string.remove_shortcut_while_token_not_found));
        }
        else
        {
            asset = token.getAssetForToken(tokenId);
            setup();
        }
    }

    private void setup()
    {
        //viewModel.checkForNewScript(token);
        //viewModel.checkTokenScriptValidity(token);
        setTitle(token.tokenInfo.name);

        if (asset != null && asset.isAttestation())
        {
            //setupAttestation(viewModel.getAssetDefinitionService().getAssetDefinition(token));
        }
        else
        {
            //viewModel.getAsset(token, tokenId);
            //viewModel.updateLocalAttributes(token, tokenId);
        }
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this)
                .get(DappBrowserViewModel.class);
        //viewModel.gasEstimateComplete().observe(this, this::checkConfirm);
        //viewModel.gasEstimateError().observe(this, this::estimateError);
        viewModel.transactionFinalised().observe(this, this::txWritten);
        viewModel.transactionSigned().observe(this, this::txSigned);
        viewModel.transactionError().observe(this, this::txError);
        //

        TokenFunctionViewModel tsViewModel = new ViewModelProvider(this)
                .get(TokenFunctionViewModel.class);
        tsViewModel.sig().observe(this, this::onSignature);

        viewModel.defaultWallet().observe(this, this::onDefaultWallet);

        viewModel.setNetwork(chainId);
        activeNetwork = viewModel.getActiveNetwork();

        viewModel.findWallet();

        /*wallet = dappViewModel.defaultWallet().getValue();

        openTokenscriptWebview(wallet);*/
    }

    private void onDefaultWallet(Wallet wallet)
    {
        this.wallet = wallet;
        if (activeNetwork != null && wallet != null)
        {
            openTokenscriptWebview(wallet);
        }
    }

    private void onSignature(XMLDsigDescriptor descriptor)
    {
        CertifiedToolbarView certificateToolbar = findViewById(R.id.certified_toolbar);
        certificateToolbar.onSigData(descriptor, this);
    }

    private void txWritten(TransactionReturn txData)
    {
        if (confirmationDialog != null && confirmationDialog.isShowing())
        {
            confirmationDialog.transactionWritten(txData.hash);
        }

        tokenScriptView.onSignTransactionSuccessful(txData);
    }

    private void txSigned(TransactionReturn txData)
    {
        confirmationDialog.transactionWritten(txData.getDisplayData());
        tokenScriptView.onSignTransactionSuccessful(txData);
    }

    //Transaction failed to be sent
    private void txError(TransactionReturn rtn)
    {
        confirmationDialog.dismiss();
        tokenScriptView.onSignCancel(rtn.tx.leafPosition);

        if (resultDialog != null && resultDialog.isShowing()) resultDialog.dismiss();
        resultDialog = new AWalletAlertDialog(this);
        resultDialog.setIcon(ERROR);
        resultDialog.setTitle(R.string.error_transaction_failed);
        resultDialog.setMessage(rtn.throwable.getMessage());
        resultDialog.setButtonText(R.string.button_ok);
        resultDialog.setButtonListener(v -> {
            resultDialog.dismiss();
        });
        resultDialog.show();

        if (confirmationDialog != null && confirmationDialog.isShowing())
            confirmationDialog.dismiss();
    }

    /*@Override
    public void showTransferToken(List<BigInteger> selection)
    {
        NFTAsset asset = token.getTokenAssets().get(tokenId);
        if (asset != null)
        {
            if (asset.isAssetMultiple())
            {
                viewModel.showTransferSelectCount(this, token, tokenId)
                        .subscribe((Consumer<Intent>) handleTransactionSuccess::launch).isDisposed();
            }
            else
            {
                if (asset.getSelectedBalance().compareTo(BigDecimal.ZERO) == 0)
                {
                    asset.setSelectedBalance(BigDecimal.ONE);
                }
                handleTransactionSuccess.launch(viewModel.getTransferIntent(this, token, singletonList(tokenId), new ArrayList<>(singletonList(asset))));
            }
        }
    }*/

    private void calculateEstimateDialog()
    {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = new AWalletAlertDialog(this);
        dialog.setTitle(getString(R.string.calc_gas_limit));
        dialog.setIcon(AWalletAlertDialog.NONE);
        dialog.setProgressMode();
        dialog.setCancelable(false);
        dialog.show();
    }

    private void estimateError(Pair<GasEstimate, Web3Transaction> estimate)
    {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(WARNING);
        dialog.setTitle(estimate.first.hasError() ?
                R.string.dialog_title_gas_estimation_failed :
                R.string.confirm_transaction
        );
        String message = estimate.first.hasError() ?
                getString(R.string.dialog_message_gas_estimation_failed, estimate.first.getError()) :
                getString(R.string.error_transaction_may_fail);
        dialog.setMessage(message);
        dialog.setButtonText(R.string.action_proceed);
        dialog.setSecondaryButtonText(R.string.action_cancel);
        dialog.setButtonListener(v -> {
            Web3Transaction w3tx = estimate.second;
            BigInteger gasEstimate = GasService.getDefaultGasLimit(token, w3tx);
            checkConfirm(new Web3Transaction(w3tx.recipient, w3tx.contract, w3tx.value, w3tx.gasPrice, gasEstimate, w3tx.nonce, w3tx.payload, w3tx.description));
        });
        dialog.setSecondaryButtonListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void checkConfirm(Web3Transaction w3tx)
    {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        confirmationDialog = new ActionSheetDialog(this, w3tx, token, "", //TODO: Reverse resolve address
                w3tx.recipient.toString(), viewModel.getTokenService(), this);
        confirmationDialog.setURL("TokenScript");
        confirmationDialog.setCanceledOnTouchOutside(false);
        confirmationDialog.show();
    }

    @Override
    public void getAuthorisation(SignAuthenticationCallback callback)
    {
        viewModel.getAuthorisation(wallet, this, callback);
    }

    @Override
    public void sendTransaction(Web3Transaction finalTx)
    {
        viewModel.requestSignature(finalTx, wallet, activeNetwork.chainId);
    }

    @Override
    public void completeSendTransaction(Web3Transaction tx, SignatureFromKey signature)
    {
        viewModel.sendTransaction(wallet, activeNetwork.chainId, tx, signature);
    }

    @Override
    public void signTransaction(Web3Transaction tx)
    {
        viewModel.requestSignatureOnly(tx, wallet, activeNetwork.chainId);
    }

    @Override
    public void completeSignTransaction(Web3Transaction w3Tx, SignatureFromKey signature)
    {
        viewModel.signTransaction(activeNetwork.chainId, w3Tx, signature);
    }


    @Override
    public void dismissed(String txHash, long callbackId, boolean actionCompleted)
    {
        //actionsheet dismissed - if action not completed then user cancelled
        if (!actionCompleted)
        {
            //actionsheet dismissed before completing signing.
            tokenScriptView.onSignCancel(callbackId);
        }
    }

    @Override
    public void notifyConfirm(String mode)
    {
        AnalyticsProperties props = new AnalyticsProperties();
        props.put(Analytics.PROPS_ACTION_SHEET_MODE, mode);
        props.put(Analytics.PROPS_ACTION_SHEET_SOURCE, ActionSheetSource.BROWSER);
    }

    @Override
    public ActivityResultLauncher<Intent> gasSelectLauncher()
    {
        return getGasSettings;
    }

    @Override
    public WalletType getWalletType()
    {
        return wallet.type;
    }

    /***
     * TokenScript view handling
     */
    private boolean openTokenscriptWebview(Wallet wallet)
    {
        boolean couldDisplay = false;
        try
        {
            LinearLayout webWrapper = findViewById(R.id.layout_webwrapper);

            tokenScriptView = findViewById(R.id.web3view);
            //tokenScriptView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            tokenScriptView.setWebChromeClient(new WebChromeClient());
            tokenScriptView.setWebViewClient(new WebViewClient());
            tokenScriptView.setChainId(activeNetwork.chainId);
            tokenScriptView.setWalletAddress(new Address(wallet.address));

            tokenScriptView.setOnSignMessageListener(this);
            tokenScriptView.setOnSignPersonalMessageListener(this);
            tokenScriptView.setOnSignTransactionListener(this);
            tokenScriptView.setOnSignTypedMessageListener(this);
            tokenScriptView.setOnEthCallListener(this);
            tokenScriptView.setOnWalletAddEthereumChainObjectListener(this);
            tokenScriptView.setOnWalletActionListener(this);

            tokenScriptView.resetView();
            tokenScriptView.loadUrl("http://192.168.1.15:3333/?viewType=alphawallet&chain=137&contract=0xD5cA946AC1c1F24Eb26dae9e1A53ba6a02bd97Fe&tokenId=3803829543");

            webWrapper.setVisibility(View.VISIBLE);

            //webWrapper.addView(tokenScriptView);
            couldDisplay = true;
            /*tokenScriptView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            if (tokenScriptView.renderTokenScriptView(token, new TicketRange(tokenId, token.getAddress()), viewModel.getAssetDefinitionService(), ViewType.VIEW, td))
            {
                webWrapper.setVisibility(View.VISIBLE);
                tokenScriptView.setChainId(token.tokenInfo.chainId);
                tokenScriptView.setWalletAddress(new Address(token.getWallet()));
                webWrapper.addView(tokenScriptView);
                couldDisplay = true;
            }*/
        }
        catch (Exception e)
        {
            //fillEmpty();
        }

        return couldDisplay;
    }


    public void onSignMessage(final EthereumMessage message)
    {
        handleSignMessage(message);
    }


    public void onSignPersonalMessage(final EthereumMessage message)
    {
        handleSignMessage(message);
    }


    public void onSignTypedMessage(@NotNull EthereumTypedMessage message)
    {
        if (message.getPrehash() == null || message.getMessageType() == SignMessageType.SIGN_ERROR)
        {
            tokenScriptView.onSignCancel(message.getCallbackId());
        }
        else
        {
            handleSignMessage(message);
        }
    }


    public void onEthCall(Web3Call call)
    {
        Timber.tag("TOKENSCRIPT").w("Received web3 request: %s", call.payload);

        Single.fromCallable(() -> {
                    //let's make the call
                    Web3j web3j = TokenRepository.getWeb3jService(activeNetwork.chainId);
                    //construct call
                    org.web3j.protocol.core.methods.request.Transaction transaction
                            = createFunctionCallTransaction(wallet.address, null, null, call.gasLimit, call.to.toString(), call.value, call.payload);
                    return web3j.ethCall(transaction, call.blockParam).send();
                }).map(EthCall::getValue)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> tokenScriptView.onCallFunctionSuccessful(call.leafPosition, result),
                        error -> tokenScriptView.onCallFunctionError(call.leafPosition, error.getMessage()))
                .isDisposed();
    }

    @Override
    public void onWalletAddEthereumChainObject(long callbackId, WalletAddEthereumChainObject chainObj)
    {
        // read chain value
        long chainId = chainObj.getChainId();
        final NetworkInfo info = viewModel.getNetworkInfo(chainId);

        // handle unknown network
        if (info == null)
        {
            // show add custom chain dialog
            addCustomChainDialog = new AddEthereumChainPrompt(this, chainObj, chainObject -> {
                if (viewModel.addCustomChain(chainObject))
                {
                    loadNewNetwork(chainObj.getChainId());
                }
                else
                {
                    displayError(R.string.error_invalid_url, 0);
                }
                addCustomChainDialog.dismiss();
            });
            addCustomChainDialog.show();
        }
        else
        {
            changeChainRequest(callbackId, info);
        }
    }

    private void loadNewNetwork(long newNetworkId)
    {
        if (activeNetwork == null || activeNetwork.chainId != newNetworkId)
        {
            viewModel.setNetwork(newNetworkId);
            viewModel.updateGasPrice(newNetworkId);
        }
        //refresh URL page
        //reloadPage();
    }

    private void displayError(int title, int text)
    {
        if (resultDialog != null && resultDialog.isShowing()) resultDialog.dismiss();
        resultDialog = new AWalletAlertDialog(this);
        resultDialog.setIcon(ERROR);
        resultDialog.setTitle(title);
        if (text != 0) resultDialog.setMessage(text);
        resultDialog.setButtonText(R.string.button_ok);
        resultDialog.setButtonListener(v -> {
            resultDialog.dismiss();
        });
        resultDialog.show();

        if (confirmationDialog != null && confirmationDialog.isShowing())
            confirmationDialog.dismiss();
    }

    private void changeChainRequest(long callbackId, NetworkInfo info)
    {
        //Don't show dialog if network doesn't need to be changed or if already showing
        if ((activeNetwork != null && activeNetwork.chainId == info.chainId) || (chainSwapDialog != null && chainSwapDialog.isShowing()))
        {
            tokenScriptView.onWalletActionSuccessful(callbackId, null);
            return;
        }

        //if we're switching between mainnet and testnet we need to pop open the 'switch to testnet' dialog (class TestNetDialog)
        // - after the user switches to testnet, go straight to switching the network (loadNewNetwork)
        // - if user is switching form testnet to mainnet, simply add the title below

        // at this stage, we know if it's testnet or not
        /*if (!info.hasRealValue() && (activeNetwork != null && activeNetwork.hasRealValue()))
        {
            TestNetDialog testnetDialog = new TestNetDialog(this, info.chainId, this);
            testnetDialog.show();
        }
        else
        {*/
        //go straight to chain change dialog
        showChainChangeDialog(callbackId, info);
        //}
    }

    @Override
    public void onRequestAccounts(long callbackId)
    {
        Timber.tag("TOKENSCRIPT").w("Received account request");
        //TODO: Pop open dialog which asks user to confirm they wish to expose their address to this dapp eg:
        //title = "Request Account Address"
        //message = "${dappUrl} requests your address. \nAuthorise?"
        //if user authorises, then do an evaluateJavascript to populate the web3.eth.getCoinbase with the current address,
        //and additionally add a window.ethereum.setAddress function in init.js to set up addresses
        //together with this update, also need to track which websites have been given permission, and if they already have it (can probably get away with using SharedPrefs)
        //then automatically perform with step without a dialog (ie same as it does currently)
        tokenScriptView.onWalletActionSuccessful(callbackId, "[\"" + wallet.address + "\"]");
    }

    //EIP-3326
    @Override
    public void onWalletSwitchEthereumChain(long callbackId, WalletAddEthereumChainObject chainObj)
    {
        //request user to change chains
        long chainId = chainObj.getChainId();

        final NetworkInfo info = viewModel.getNetworkInfo(chainId);

        if (info == null)
        {
            chainSwapDialog = new AWalletAlertDialog(this);
            chainSwapDialog.setTitle(R.string.unknown_network_title);
            chainSwapDialog.setMessage(getString(R.string.unknown_network, String.valueOf(chainId)));
            chainSwapDialog.setButton(R.string.dialog_ok, v -> {
                if (chainSwapDialog.isShowing()) chainSwapDialog.dismiss();
            });
            chainSwapDialog.setSecondaryButton(R.string.action_cancel, v -> chainSwapDialog.dismiss());
            chainSwapDialog.setCancelable(false);
            chainSwapDialog.show();
        }
        else
        {
            changeChainRequest(callbackId, info);
        }
    }

    /**
     * This will pop the ActionSheetDialog to request a chain change, with appropriate warning
     * if switching between mainnets and testnets
     *
     * @param callbackId
     * @param newNetwork
     */
    private void showChainChangeDialog(long callbackId, NetworkInfo newNetwork)
    {
        Token baseToken = viewModel.getTokenService().getTokenOrBase(newNetwork.chainId, wallet.address);
        confirmationDialog = new ActionSheetDialog(this, this, R.string.switch_chain_request, R.string.switch_and_reload,
                callbackId, baseToken, activeNetwork, newNetwork);
        confirmationDialog.setCanceledOnTouchOutside(true);
        confirmationDialog.show();
        confirmationDialog.fullExpand();
    }

    private void handleSignMessage(Signable message)
    {
        if (message.getMessageType() == SignMessageType.SIGN_TYPED_DATA_V3 && message.getChainId() != activeNetwork.chainId)
        {
            showErrorDialogIncompatibleNetwork(message.getCallbackId(), message.getChainId(), activeNetwork.chainId);
        }
        else if (confirmationDialog == null || !confirmationDialog.isShowing())
        {
            confirmationDialog = new ActionSheetSignDialog(this, this, message);
            confirmationDialog.show();
        }
    }

    private void showErrorDialogIncompatibleNetwork(long callbackId, long requestingChainId, long activeChainId)
    {
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
        {
            errorDialog = new AWalletAlertDialog(this, AWalletAlertDialog.ERROR);
            String message = com.alphawallet.app.repository.EthereumNetworkBase.isChainSupported(requestingChainId) ?
                    getString(R.string.error_eip712_incompatible_network,
                            com.alphawallet.app.repository.EthereumNetworkBase.getShortChainName(requestingChainId),
                            com.alphawallet.app.repository.EthereumNetworkBase.getShortChainName(activeChainId)) :
                    getString(R.string.error_eip712_unsupported_network, String.valueOf(requestingChainId));
            errorDialog.setMessage(message);
            errorDialog.setButton(R.string.action_cancel, v -> {
                errorDialog.dismiss();
                dismissed("", callbackId, false);
            });
            errorDialog.setCancelable(false);
            errorDialog.show();

            viewModel.trackError(Analytics.Error.BROWSER, message);
        }
    }

    @Override
    public void signingComplete(SignatureFromKey signature, Signable message)
    {
        String signHex = Numeric.toHexString(signature.signature);
        Timber.d("Initial Msg: %s", message.getMessage());
        confirmationDialog.success();
        tokenScriptView.onSignMessageSuccessful(message, signHex);
    }

    @Override
    public void signingFailed(Throwable error, Signable message)
    {
        tokenScriptView.onSignCancel(message.getCallbackId());
        confirmationDialog.dismiss();
    }

    @Override
    public void onSignTransaction(Web3Transaction transaction, String url)
    {
        try
        {
            //minimum for transaction to be valid: recipient and value or payload
            if ((confirmationDialog == null || !confirmationDialog.isShowing()) &&
                    (transaction.recipient.equals(Address.EMPTY) && transaction.payload != null) // Constructor
                    || (!transaction.recipient.equals(Address.EMPTY) && (transaction.payload != null || transaction.value != null))) // Raw or Function TX
            {
                Token token = viewModel.getTokenService().getTokenOrBase(activeNetwork.chainId, transaction.recipient.toString());
                confirmationDialog = new ActionSheetDialog(this, transaction, token,
                        "", transaction.recipient.toString(), viewModel.getTokenService(), this);
                ((ActionSheetDialog) confirmationDialog).setDappSigningMode();
                confirmationDialog.setURL(url);
                confirmationDialog.setCanceledOnTouchOutside(false);
                confirmationDialog.show();
                confirmationDialog.fullExpand();

                viewModel.calculateGasEstimate(wallet, transaction, activeNetwork.chainId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(estimate -> confirmationDialog.setGasEstimate(estimate),
                                Throwable::printStackTrace)
                        .isDisposed();

                return;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        onInvalidTransaction(transaction);
        tokenScriptView.onSignCancel(transaction.leafPosition);
    }

    private void onInvalidTransaction(Web3Transaction transaction)
    {
        resultDialog = new AWalletAlertDialog(this);
        resultDialog.setIcon(AWalletAlertDialog.ERROR);
        resultDialog.setTitle(getString(R.string.invalid_transaction));

        if (transaction.recipient.equals(Address.EMPTY) && (transaction.payload == null || transaction.value != null))
        {
            resultDialog.setMessage(getString(R.string.contains_no_recipient));
        }
        else if (transaction.payload == null && transaction.value == null)
        {
            resultDialog.setMessage(getString(R.string.contains_no_value));
        }
        else
        {
            resultDialog.setMessage(getString(R.string.contains_no_data));
        }
        resultDialog.setButtonText(R.string.button_ok);
        resultDialog.setButtonListener(v -> {
            resultDialog.dismiss();
        });
        resultDialog.setCancelable(true);
        resultDialog.show();
    }

}
