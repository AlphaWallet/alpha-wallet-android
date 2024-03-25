package com.alphawallet.app.widget;

import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.ActionSheetInterface;
import com.alphawallet.app.entity.ActionSheetStatus;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.EIP1559FeeOracleResult;
import com.alphawallet.app.entity.GasEstimate;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.TXSpeed;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.analytics.ActionSheetMode;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.EthereumNetworkBase;
import com.alphawallet.app.repository.SharedPreferenceRepository;
import com.alphawallet.app.repository.entity.Realm1559Gas;
import com.alphawallet.app.repository.entity.RealmTransaction;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.HomeActivity;
import com.alphawallet.app.ui.TransactionSuccessActivity;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.ui.widget.entity.GasWidgetInterface;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.walletconnect.entity.WCPeerMeta;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.hardware.SignatureFromKey;
import com.alphawallet.hardware.SignatureReturnType;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

/**
 * Created by JB on 17/11/2020.
 */
public class ActionSheetDialog extends ActionSheet implements StandardFunctionInterface, ActionSheetInterface
{
    private final BottomSheetToolbarView toolbar;
    private final GasWidget2 gasWidget;
    private final GasWidget gasWidgetLegacy;
    private final BalanceDisplayWidget balanceDisplay;
    private final NetworkDisplayWidget networkDisplay;
    private final ConfirmationWidget confirmationWidget;
    private final AddressDetailView senderAddressDetail;
    private final AddressDetailView receiptAddressDetail;
    private final AmountDisplayWidget amountDisplay;
    private final AssetDetailView assetDetailView;
    private final FunctionButtonBar functionBar;
    private final TransactionDetailWidget detailWidget;
    private final WalletConnectRequestWidget walletConnectRequestWidget;
    private final Activity activity;
    private final GasWidgetInterface gasWidgetInterface;

    private final Token token;
    private final TokensService tokensService;

    private final Web3Transaction candidateTransaction; //initial skeleton transaction from hosting activity
    private Web3Transaction establishedTransaction;     //final transaction to be submitted to blockchain
    private final ActionSheetCallback actionSheetCallback;
    private final long callbackId;
    private SignAuthenticationCallback signCallback;
    private ActionSheetMode mode;
    private String txHash = null;
    private boolean actionCompleted;
    private boolean use1559Transactions = false;
    private Transaction transaction;
    private final WalletType walletType;
    @Nullable
    private Disposable backupGasFetchDisposable;

    private EIP1559FeeOracleResult gasToUse = null;

    public ActionSheetDialog(@NonNull Activity activity, Web3Transaction tx, Token t,
                             String destName, String destAddress, TokensService ts,
                             ActionSheetCallback aCallBack)
    {
        super(activity);
        View view = View.inflate(getContext(), R.layout.dialog_action_sheet, null);
        setContentView(view);

        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from((View) view.getParent());
        behavior.setState(STATE_EXPANDED);
        behavior.setSkipCollapsed(true);

        toolbar = findViewById(R.id.bottom_sheet_toolbar);
        gasWidget = findViewById(R.id.gas_widgetx);
        gasWidgetLegacy = findViewById(R.id.gas_widget_legacy);
        balanceDisplay = findViewById(R.id.balance);
        networkDisplay = findViewById(R.id.network_display_widget);
        confirmationWidget = findViewById(R.id.confirmation_view);
        detailWidget = findViewById(R.id.detail_widget);
        senderAddressDetail = findViewById(R.id.sender);
        receiptAddressDetail = findViewById(R.id.recipient);
        amountDisplay = findViewById(R.id.amount_display);
        assetDetailView = findViewById(R.id.asset_detail);
        functionBar = findViewById(R.id.layoutButtons);
        this.activity = activity;
        if (activity instanceof HomeActivity)
        {
            mode = ActionSheetMode.SEND_TRANSACTION_DAPP;
        }
        else
        {
            mode = ActionSheetMode.SEND_TRANSACTION;
        }

        signCallback = null;
        walletConnectRequestWidget = null;

        actionSheetCallback = aCallBack;
        actionCompleted = false;

        token = t;
        tokensService = ts;
        candidateTransaction = tx;
        callbackId = tx.leafPosition;

        transaction = new Transaction(tx, token.tokenInfo.chainId, ts.getCurrentAddress());
        transaction.transactionInput = Transaction.decoder.decodeInput(candidateTransaction, token.tokenInfo.chainId, token.getWallet());

        balanceDisplay.setupBalance(token, transaction);
        networkDisplay.setNetwork(token.tokenInfo.chainId);

        gasWidgetInterface = setupGasWidget();

        //ensure gas service is started
        actionSheetCallback.getGasService().startGasPriceCycle(token.tokenInfo.chainId);

        if (!tx.gasLimit.equals(BigInteger.ZERO))
        {
            setGasEstimate(new GasEstimate(tx.gasLimit));
        }

        updateAmount();

        senderAddressDetail.setupAddress(token.getWallet(), "", null);
        receiptAddressDetail.setupAddress(destAddress, destName, tokensService.getToken(token.tokenInfo.chainId, destAddress));

        if (token.isNonFungible())
        {
            balanceDisplay.setVisibility(View.GONE);

            if (token.getInterfaceSpec() == ContractType.ERC1155)
            {
                List<NFTAsset> assetList = token.getAssetListFromTransaction(transaction);
                amountDisplay.setVisibility(View.VISIBLE);
                amountDisplay.setAmountFromAssetList(assetList);
            }
            else
            {
                amountDisplay.setVisibility(View.GONE);
                assetDetailView.setupAssetDetail(token, getERC721TokenId(), this);
                assetDetailView.setVisibility(View.VISIBLE);
            }
        }

        setupTransactionDetails();

        setupCancelListeners();

        walletType = actionSheetCallback.getWalletType();

        if (walletType == WalletType.HARDWARE)
        {
            functionBar.setupFunctions(this, new ArrayList<>(Collections.singletonList(R.string.use_hardware_card)));
            functionBar.setClickable(false);
            //push signing request
            handleTransactionOperation();
        }
        else
        {
            functionBar.setupFunctions(this, new ArrayList<>(Collections.singletonList(R.string.action_confirm)));
        }

        functionBar.revealButtons();
    }

    // wallet connect request
    public ActionSheetDialog(Activity activity, WCPeerMeta wcPeerMeta, long chainIdOverride, String iconUrl, ActionSheetCallback actionSheetCallback)
    {
        super(activity);
        setContentView(R.layout.dialog_wallet_connect_sheet);
        mode = ActionSheetMode.WALLET_CONNECT_REQUEST;

        functionBar = findViewById(R.id.layoutButtons);

        toolbar = findViewById(R.id.bottom_sheet_toolbar);

        this.activity = activity;
        this.actionSheetCallback = actionSheetCallback;

        walletConnectRequestWidget = findViewById(R.id.wallet_connect_widget);
        gasWidget = null;
        balanceDisplay = null;
        networkDisplay = null;
        confirmationWidget = null;
        senderAddressDetail = null;
        receiptAddressDetail = null;
        amountDisplay = null;
        assetDetailView = null;
        detailWidget = null;
        token = null;
        tokensService = null;
        candidateTransaction = null;
        callbackId = 0;
        gasWidgetLegacy = null;
        gasWidgetInterface = null;
        walletType = actionSheetCallback.getWalletType();

        toolbar.setLogo(activity, iconUrl);
        toolbar.setTitle(wcPeerMeta.getName());
        toolbar.setCloseListener(v -> actionSheetCallback.denyWalletConnect());

        walletConnectRequestWidget.setupWidget(wcPeerMeta, chainIdOverride, actionSheetCallback::openChainSelection);

        ArrayList<Integer> functionList = new ArrayList<>();
        functionList.add(R.string.approve);
        functionList.add(R.string.dialog_reject);
        functionBar.setupFunctions(this, functionList);
        functionBar.revealButtons();

        setActionSheetStatus(EthereumNetworkBase.isChainSupported(chainIdOverride) ?
            ActionSheetStatus.OK : ActionSheetStatus.ERROR_INVALID_CHAIN);
    }

    // switch chain
    public ActionSheetDialog(Activity activity, ActionSheetCallback aCallback, int titleId, int buttonTextId,
                             long cId, Token baseToken, NetworkInfo oldNetwork, NetworkInfo newNetwork)
    {
        super(activity);
        setContentView(R.layout.dialog_action_sheet_switch_chain);

        toolbar = findViewById(R.id.bottom_sheet_toolbar);
        SwitchChainWidget switchChainWidget = findViewById(R.id.switch_chain_widget);
        switchChainWidget.setupSwitchChainData(oldNetwork, newNetwork);

        functionBar = findViewById(R.id.layoutButtons);
        this.activity = activity;

        actionSheetCallback = aCallback;
        mode = ActionSheetMode.MESSAGE;

        toolbar.setTitle(titleId);

        gasWidget = null;
        balanceDisplay = null;
        networkDisplay = null;
        confirmationWidget = null;
        senderAddressDetail = null;
        receiptAddressDetail = null;
        amountDisplay = null;
        assetDetailView = null;
        detailWidget = null;
        callbackId = cId;
        token = baseToken;
        tokensService = null;
        candidateTransaction = null;
        walletConnectRequestWidget = null;
        gasWidgetLegacy = null;
        gasWidgetInterface = null;
        walletType = actionSheetCallback.getWalletType();

        functionBar.setupFunctions(this, new ArrayList<>(Collections.singletonList(buttonTextId)));
        functionBar.revealButtons();
        setupCancelListeners();
    }

    public ActionSheetDialog(Activity activity, ActionSheetMode mode)
    {
        super(activity);
        this.activity = activity;
        this.mode = mode;
        if (mode == ActionSheetMode.NODE_STATUS_INFO)
        {
            setContentView(R.layout.dialog_action_sheet_node_status);
        }
        toolbar = null;
        gasWidget = null;
        gasWidgetLegacy = null;
        balanceDisplay = null;
        networkDisplay = null;
        confirmationWidget = null;
        senderAddressDetail = null;
        receiptAddressDetail = null;
        amountDisplay = null;
        assetDetailView = null;
        functionBar = null;
        detailWidget = null;
        walletConnectRequestWidget = null;
        gasWidgetInterface = null;
        token = null;
        tokensService = null;
        candidateTransaction = null;
        actionSheetCallback = null;
        walletType = WalletType.NOT_DEFINED;
        callbackId = 0;
    }

    private GasWidgetInterface setupGasWidget()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean canUse1559Transactions = prefs.getBoolean(SharedPreferenceRepository.EXPERIMENTAL_1559_TX, true);

        use1559Transactions = canUse1559Transactions && has1559Gas() //1559 Transactions toggled on in settings and this chain supports 1559
                && !(token.isEthereum() && candidateTransaction.leafPosition == -2) //User not sweeping wallet (if so we need to use legacy tx)
                && !tokensService.hasLockedGas(token.tokenInfo.chainId); //Service has locked gas, can only use legacy (eg Optimism).

        if (use1559Transactions)
        {
            gasWidget.setupWidget(tokensService, token, candidateTransaction, this);
            return gasWidget;
        }
        else
        {
            gasWidget.setVisibility(View.GONE);
            gasWidgetLegacy.setVisibility(View.VISIBLE);
            gasWidgetLegacy.setupWidget(tokensService, token, candidateTransaction, this, this);
            return gasWidgetLegacy;
        }
    }

    public void setSignOnly()
    {
        //sign only, and return signature to process
        mode = ActionSheetMode.SIGN_TRANSACTION;
        toolbar.setTitle(R.string.dialog_title_sign_transaction);
        use1559Transactions = !candidateTransaction.isLegacyTransaction(); // If doing a sign-only transaction (not send) we must respect ERC-1559 or legacy.
    }

    public void setDappSigningMode()
    {
        this.mode = ActionSheetMode.SEND_TRANSACTION_DAPP;
    }

    public void onDestroy()
    {
        if (gasWidgetInterface != null) gasWidgetInterface.onDestroy();
        if (assetDetailView != null) assetDetailView.onDestroy();
        if (detailWidget != null) detailWidget.onDestroy();
    }

    public void setURL(String url)
    {
        AddressDetailView requester = findViewById(R.id.requester);
        requester.setupRequester(url);
        setupTransactionDetails();

        if (candidateTransaction.isConstructor())
        {
            receiptAddressDetail.setVisibility(View.GONE);
        }

        if (candidateTransaction.value.compareTo(BigInteger.ZERO) > 0 || candidateTransaction.isBaseTransfer())
        {
            amountDisplay.setVisibility(View.VISIBLE);
            amountDisplay.setAmountUsingToken(candidateTransaction.value, tokensService.getServiceToken(token.tokenInfo.chainId), tokensService);
        }
        else
        {
            amountDisplay.setVisibility(View.GONE);
        }
    }

    private void setupTransactionDetails()
    {
        if (candidateTransaction.isBaseTransfer())
        {
            detailWidget.setVisibility(View.GONE);
        }
        else
        {
            detailWidget.setVisibility(View.VISIBLE);
            detailWidget.setupTransaction(candidateTransaction, token.tokenInfo.chainId,
                    tokensService.getNetworkSymbol(token.tokenInfo.chainId), this);
        }
    }

    @Override
    public void setCurrentGasIndex(ActivityResult result)
    {
        if (result == null || result.getData() == null) return;
        int gasSelectionIndex = result.getData().getIntExtra(C.EXTRA_SINGLE_ITEM, TXSpeed.STANDARD.ordinal());
        long customNonce = result.getData().getLongExtra(C.EXTRA_NONCE, -1);
        BigInteger maxFeePerGas = result.getData().hasExtra(C.EXTRA_GAS_PRICE) ?
                new BigInteger(result.getData().getStringExtra(C.EXTRA_GAS_PRICE)) : BigInteger.ZERO;
        BigInteger maxPriorityFee = result.getData().hasExtra(C.EXTRA_MIN_GAS_PRICE) ?
                new BigInteger(result.getData().getStringExtra(C.EXTRA_MIN_GAS_PRICE)) : BigInteger.ZERO;
        BigDecimal customGasLimit = new BigDecimal(result.getData().getStringExtra(C.EXTRA_GAS_LIMIT));
        long expectedTxTime = result.getData().getLongExtra(C.EXTRA_AMOUNT, 0);
        gasWidgetInterface.setCurrentGasIndex(gasSelectionIndex, maxFeePerGas, maxPriorityFee, customGasLimit, expectedTxTime, customNonce);
    }

    private boolean isSendingTransaction()
    {
        return (mode != ActionSheetMode.SIGN_MESSAGE && mode != ActionSheetMode.SIGN_TRANSACTION);
    }

    public void setupResendTransaction(ActionSheetMode callingMode)
    {
        mode = callingMode;
        gasWidgetInterface.setupResendSettings(mode, candidateTransaction.gasPrice);
        balanceDisplay.setVisibility(View.GONE);
        networkDisplay.setVisibility(View.GONE);
        receiptAddressDetail.setVisibility(View.GONE);
        detailWidget.setVisibility(View.GONE);
        amountDisplay.setVisibility(View.GONE);
    }

    @Override
    public void updateAmount()
    {
        showAmount(getTransactionAmount().toBigInteger());
    }


    @Override
    @SuppressWarnings("checkstyle:MissingSwitchDefault")
    public void handleClick(String action, int id)
    {
        //first ensure gas estimate is up to date

        if (walletType == WalletType.HARDWARE)
        {
            //TODO: Hardware - Maybe flick a toast to tell user to apply card
            return;
        }

        switch (mode)
        {
            case SEND_TRANSACTION_WC:
            case SEND_TRANSACTION:
            case SEND_TRANSACTION_DAPP:
            case SPEEDUP_TRANSACTION:
            case CANCEL_TRANSACTION:
                gasToUse = getGasSettingsToUse();
                if (gasToUse == null)
                {
                    functionBar.setPrimaryButtonWaiting();
                    //call gas collect then push tx again
                    callGasUpdateAndRePushTx();
                }
                else
                {
                    checkGasAndSend();
                }
                break;
            case SIGN_TRANSACTION:
                handleTransactionOperation();
                break;
            case MESSAGE:
                actionSheetCallback.buttonClick(callbackId, token);
                break;
            case WALLET_CONNECT_REQUEST:
                if (id == R.string.approve)
                {
                    actionSheetCallback.notifyWalletConnectApproval(walletConnectRequestWidget.getChainIdOverride());
                    tryDismiss();
                }
                else
                {
                    actionSheetCallback.denyWalletConnect();
                }
                break;
        }
    }

    private void checkGasAndSend()
    {
        functionBar.setPrimaryButtonEnabled(true);
        //check gas and warn user
        if (!gasWidgetInterface.checkSufficientGas())
        {
            askUserForInsufficientGasConfirm();
        }
        else
        {
            handleTransactionOperation();
        }
    }

    private void callGasUpdateAndRePushTx()
    {
        //fetch gas and then re-click
        backupGasFetchDisposable = actionSheetCallback.getGasService().fetchGasPrice(token.tokenInfo.chainId, use1559Transactions)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(gasPriceStandard -> {
                gasToUse = gasPriceStandard;
                if (use1559Transactions && gasToUse.maxFeePerGas.equals(BigInteger.ZERO) && gasToUse.priorityFee.equals(BigInteger.ZERO))
                {
                    use1559Transactions = false;
                }
                checkGasAndSend();
            });
    }

    @Override
    public void gasEstimateReady()
    {
        functionBar.setPrimaryButtonEnabled(true);
    }

    @Override
    public ActivityResultLauncher<Intent> gasSelectLauncher()
    {
        return actionSheetCallback.gasSelectLauncher();
    }

    private EIP1559FeeOracleResult getGasSettingsToUse()
    {
        if (gasWidgetInterface != null && gasWidgetInterface.gasPriceReady())
        {
            if (use1559Transactions)
            {
                return new EIP1559FeeOracleResult(gasWidgetInterface.getGasMax(),
                        gasWidgetInterface.getPriorityFee(), gasWidgetInterface.getGasPrice());
            }
            else
            {
                return new EIP1559FeeOracleResult(BigInteger.ZERO,
                        BigInteger.ZERO, gasWidgetInterface.getGasPrice(candidateTransaction.gasPrice));
            }
        }

        return null;
    }

    private BigDecimal getTransactionAmount()
    {
        BigDecimal txAmount;
        if (token.isEthereum())
        {
            txAmount = new BigDecimal(gasWidgetInterface.getValue());
        }
        else if (isSendingTransaction())
        {
            txAmount = new BigDecimal(token.getTransferValueRaw(transaction.transactionInput));
        }
        else
        {
            txAmount = BigDecimal.ZERO;
        }

        return txAmount;
    }

    private String getERC721TokenId()
    {
        if (!token.isERC721())
        {
            return "";
        }
        else if (actionSheetCallback.getTokenId().compareTo(BigInteger.ZERO) > 0)
        {
            return actionSheetCallback.getTokenId().toString();
        }
        else
        {
            return token.getTransferValueRaw(transaction.transactionInput).toString();
        }
    }

    /**
     * Popup a dialogbox to ask user if they really want to try to send this transaction,
     * as we calculate it will fail due to insufficient gas. User knows best though.
     */
    private void askUserForInsufficientGasConfirm()
    {
        AWalletAlertDialog dialog = new AWalletAlertDialog(getContext());
        dialog.setIcon(AWalletAlertDialog.WARNING);
        dialog.setTitle(R.string.insufficient_gas);
        dialog.setMessage(getContext().getString(R.string.not_enough_gas_message));
        dialog.setButtonText(R.string.action_send);
        dialog.setSecondaryButtonText(R.string.cancel_transaction);
        dialog.setButtonListener(v -> {
            dialog.dismiss();
            handleTransactionOperation();
        });
        dialog.setSecondaryButtonListener(v -> {
            dialog.dismiss();
        });
        dialog.show();
    }

    public void transactionWritten(String tx)
    {
        txHash = tx;
        //dismiss on message completion
        confirmationWidget.completeProgressMessage(txHash, this::showTransactionSuccess);
        if (!TextUtils.isEmpty(tx) && tx.startsWith("0x"))
        {
            updateRealmTransactionFinishEstimate(tx);
        }
    }

    @SuppressWarnings("checkstyle:MissingSwitchDefault")
    private void showTransactionSuccess()
    {
        switch (mode)
        {
            case SEND_TRANSACTION:
                //Display transaction success dialog
                Intent intent = new Intent(getContext(), TransactionSuccessActivity.class);
                intent.putExtra(C.EXTRA_TXHASH, txHash);
                intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                activity.startActivityForResult(intent, C.COMPLETED_TRANSACTION);
                tryDismiss();
                break;

            case SEND_TRANSACTION_WC:
            case SEND_TRANSACTION_DAPP:
            case SPEEDUP_TRANSACTION:
            case CANCEL_TRANSACTION:
            case SIGN_TRANSACTION:
                //return to dapp
                tryDismiss();
                break;
        }
    }

    private void tryDismiss()
    {
        if (Utils.stillAvailable(activity) && isShowing()) dismiss();
    }

    private void updateRealmTransactionFinishEstimate(String txHash)
    {
        final long expectedTime = System.currentTimeMillis() + gasWidgetInterface.getExpectedTransactionTime() * 1000;
        try (Realm realm = tokensService.getWalletRealmInstance())
        {
            realm.executeTransactionAsync(r -> {
                RealmTransaction rt = r.where(RealmTransaction.class)
                        .equalTo("hash", txHash)
                        .findFirst();

                if (rt != null)
                {
                    rt.setExpectedCompletion(expectedTime);
                    r.insertOrUpdate(rt);
                }
            });
        }
        catch (Exception e)
        {
            //
        }
    }

    private void setupCancelListeners()
    {
        toolbar.setCloseListener(v -> dismiss());

        setOnDismissListener(v -> {
            actionSheetCallback.dismissed(txHash, callbackId, actionCompleted);
            if (gasWidgetInterface != null) gasWidgetInterface.onDestroy();
            if (backupGasFetchDisposable != null && !backupGasFetchDisposable.isDisposed())
            {
                backupGasFetchDisposable.dispose();
            }
        });
    }

    @SuppressWarnings("checkstyle:MissingSwitchDefault")
    public void completeSignRequest(boolean gotAuth)
    {
        if (signCallback != null)
        {
            actionCompleted = true;

            switch (mode)
            {
                case SEND_TRANSACTION_WC:
                case SEND_TRANSACTION:
                case SEND_TRANSACTION_DAPP:
                case SPEEDUP_TRANSACTION:
                case CANCEL_TRANSACTION:
                case SIGN_TRANSACTION:
                    signCallback.gotAuthorisation(gotAuth);
                    break;

                case SIGN_MESSAGE:
                    //display success and hand back to calling function
                    confirmationWidget.startProgressCycle(1);
                    signCallback.gotAuthorisation(gotAuth);
                    break;
            }
        }
    }

    private Web3Transaction formTransaction()
    {
        //form Web3Transaction
        if (!use1559Transactions)
        {
            establishedTransaction = new Web3Transaction(
                    candidateTransaction.recipient,
                    candidateTransaction.contract,
                    candidateTransaction.sender,
                    gasWidgetInterface.getValue(),
                    gasToUse.baseFee,
                    gasWidgetInterface.getGasLimit(),
                    gasWidgetInterface.getNonce(),
                    candidateTransaction.payload,
                    candidateTransaction.leafPosition
            );
        }
        else
        {
            establishedTransaction = new Web3Transaction(
                    candidateTransaction.recipient,
                    candidateTransaction.contract,
                    candidateTransaction.sender,
                    gasWidgetInterface.getValue(),
                    gasToUse.maxFeePerGas,
                    gasToUse.priorityFee,
                    gasWidgetInterface.getGasLimit(),
                    gasWidgetInterface.getNonce(),
                    candidateTransaction.payload,
                    candidateTransaction.leafPosition
            );
        }

        return establishedTransaction;
    }

    /**
     * Either Send or Sign (WalletConnect only) the transaction
     */
    private void handleTransactionOperation()
    {
        if (walletType != WalletType.HARDWARE)
        {
            functionBar.setVisibility(View.GONE);
        }

        //get approval and push transaction
        //authentication screen
        signCallback = new SignAuthenticationCallback()
        {
            @Override
            public void gotAuthorisation(boolean gotAuth)
            {
                actionCompleted = true;
                if (!gotAuth)
                {
                    cancelAuthentication();
                    return;
                }
                if (walletType != WalletType.HARDWARE)
                {
                    confirmationWidget.startProgressCycle(4);
                }

                switch (mode)
                {
                    case SEND_TRANSACTION:
                    case SEND_TRANSACTION_DAPP:
                    case SEND_TRANSACTION_WC:
                    case SPEEDUP_TRANSACTION:
                    case CANCEL_TRANSACTION:
                        actionSheetCallback.sendTransaction(formTransaction());
                        break;
                    case SIGN_TRANSACTION:
                        actionSheetCallback.signTransaction(formTransaction());
                        break;
                    case MESSAGE:
                    case SIGN_MESSAGE:
                    case WALLET_CONNECT_REQUEST:
                    case NODE_STATUS_INFO:
                    default:
                        break;
                }

                actionSheetCallback.notifyConfirm(mode.getValue());
            }

            @Override
            public void cancelAuthentication()
            {
                confirmationWidget.hide();
                functionBar.setVisibility(View.VISIBLE);
            }

            /**
             * Return from hardware sign
             * @param signature
             */
            @Override
            public void gotSignature(SignatureFromKey signature)
            {
                if (signature.sigType == SignatureReturnType.SIGNATURE_GENERATED)
                {
                    functionBar.setVisibility(View.GONE);
                    confirmationWidget.startProgressCycle(4);
                    if (mode == ActionSheetMode.SIGN_TRANSACTION)
                    {
                        actionSheetCallback.completeSignTransaction(establishedTransaction, signature);
                    }
                    else
                    {
                        actionSheetCallback.completeSendTransaction(establishedTransaction, signature);
                    }
                }
                else
                {
                    //TODO: Hardware - report error in a better way
                    activity.runOnUiThread(() -> Toast.makeText(activity, "ERROR: " + signature.failMessage, Toast.LENGTH_SHORT).show());
                }
            }
        };

        actionSheetCallback.getAuthorisation(signCallback);
    }

    @Override
    public void setGasEstimate(GasEstimate estimate)
    {
        if (!TextUtils.isEmpty(estimate.getError())) // Display error
        {

        }
        else
        {
            gasWidgetInterface.setGasEstimate(estimate.getValue());
            functionBar.setPrimaryButtonEnabled(true);
        }
    }

    private void showAmount(BigInteger amountVal)
    {
        amountDisplay.setAmountUsingToken(amountVal, token, tokensService);

        BigInteger networkFee = gasWidgetInterface.getGasPrice(candidateTransaction.gasPrice).multiply(gasWidgetInterface.getGasLimit());
        BigInteger balanceAfterTransaction = token.balance.toBigInteger().subtract(gasWidgetInterface.getValue());
        balanceDisplay.setNewBalanceText(token, getTransactionAmount(), networkFee, balanceAfterTransaction);
    }

    public void success()
    {
        if (!activity.isFinishing() && Utils.stillAvailable(activity) && isShowing())
        {
            confirmationWidget.completeProgressMessage(".", this::dismiss);
        }
    }

    public void waitForEstimate()
    {
        functionBar.setPrimaryButtonWaiting();
    }

    public void updateChain(long chainId)
    {
        walletConnectRequestWidget.updateChain(chainId);
    }

    public Web3Transaction getTransaction()
    {
        return candidateTransaction;
    }

    private boolean has1559Gas()
    {
        try (Realm realm = tokensService.getTickerRealmInstance())
        {
            Realm1559Gas rgs = realm.where(Realm1559Gas.class)
                    .equalTo("chainId", token.tokenInfo.chainId)
                    .findFirst();

            if (rgs != null)
            {
                return true;
            }
        }
        catch (Exception e)
        {
            //
        }

        return false;
    }

    public void setSigningWallet(String address)
    {
        SharedPreferenceRepository prefs = new SharedPreferenceRepository(getContext());
        if (!prefs.getCurrentWalletAddress().equalsIgnoreCase(address))
        {
            receiptAddressDetail.addMessage(getContext().getString(R.string.message_wc_wallet_different_from_active_wallet), R.drawable.ic_red_warning);
        }
    }
}
