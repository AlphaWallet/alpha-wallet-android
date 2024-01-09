package com.alphawallet.app.ui;

import static android.text.Html.FROM_HTML_MODE_LEGACY;
import static com.alphawallet.app.widget.AWalletAlertDialog.ERROR;
import static com.alphawallet.app.widget.AWalletAlertDialog.WARNING;
import static org.web3j.protocol.core.methods.request.Transaction.createFunctionCallTransaction;
import static java.util.Collections.singletonList;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.analytics.Analytics;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.GasEstimate;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.TransactionReturn;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.opensea.OpenSeaAsset;
import com.alphawallet.app.entity.tokens.Attestation;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.ui.widget.entity.NFTAttributeLayout;
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
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.app.widget.NFTImageView;
import com.alphawallet.app.widget.TokenInfoCategoryView;
import com.alphawallet.app.widget.TokenInfoView;
import com.alphawallet.ethereum.EthereumNetworkBase;
import com.alphawallet.hardware.SignatureFromKey;
import com.alphawallet.token.entity.EthereumMessage;
import com.alphawallet.token.entity.EthereumTypedMessage;
import com.alphawallet.token.entity.SignMessageType;
import com.alphawallet.token.entity.Signable;
import com.alphawallet.token.entity.TSAction;
import com.alphawallet.token.entity.TokenScriptResult.Attribute;
import com.alphawallet.token.entity.XMLDsigDescriptor;
import com.alphawallet.token.tools.TokenDefinition;

import org.jetbrains.annotations.NotNull;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@AndroidEntryPoint
public class NFTAssetDetailActivityJs extends BaseActivity implements StandardFunctionInterface, ActionSheetCallback,
        OnSignTransactionListener, OnSignPersonalMessageListener, OnSignTypedMessageListener, OnSignMessageListener,
        OnEthCallListener, OnWalletAddEthereumChainObjectListener, OnWalletActionListener
{
    private TokenFunctionViewModel viewModel;
    private Token token;
    private BigInteger tokenId;
    private NFTAsset asset;
    private String sequenceId;
    private ActionSheet confirmationDialog;
    private AWalletAlertDialog dialog;
    private NFTImageView tokenImage;
    private NFTAttributeLayout nftAttributeLayout;
    private NFTAttributeLayout tsAttributeLayout;
    private TextView tokenDescription;
    private ActionMenuItemView refreshMenu;
    private ProgressBar progressBar;
    private TokenInfoCategoryView descriptionLabel;
    private TokenInfoView tivTokenId;
    private TokenInfoView tivNetwork;
    private TokenInfoView tivContractAddress;
    private TokenInfoView tivBalance;
    private TokenInfoView tivName;
    private TokenInfoView tivExternalLink;
    private TokenInfoView tivCreator;
    private TokenInfoView tivTokenStandard;
    private TokenInfoView tivTotalSupply;
    private TokenInfoView tivNumOwners;
    private TokenInfoView tivOwner;
    private TokenInfoView tivLastSale;
    private TokenInfoView tivAveragePrice;
    private TokenInfoView tivFloorPrice;
    private TokenInfoView tivRarityData;
    private Animation rotation;
    private ActivityResultLauncher<Intent> handleTransactionSuccess;
    private ActivityResultLauncher<Intent> getGasSettings;
    private boolean triggeredReload;
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
        setContentView(R.layout.activity_nft_asset_detail);

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
            progressBar.setVisibility(View.VISIBLE);
            getIntentData();
            tokenImage.onResume();
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
        tokenImage.onDestroy();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        tokenImage.onPause();
        if (tokenScriptView != null && tokenScriptView.getVisibility() == View.VISIBLE)
        {
            LinearLayout webWrapper = findViewById(R.id.layout_webwrapper);
            webWrapper.removeView(tokenScriptView);
            tokenScriptView.destroy();
            tokenScriptView = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu)
    {
        if (asset == null || !asset.isAttestation())
        {
            getMenuInflater().inflate(R.menu.menu_refresh, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.action_reload_metadata)
        {
            reloadMetadata();
        }
        return super.onOptionsItemSelected(item);
    }

    private void initViews()
    {
        tokenImage = findViewById(R.id.asset_image);
        nftAttributeLayout = findViewById(R.id.attributes);
        //tsAttributeLayout = findViewById(R.id.ts_attributes);
        tokenDescription = findViewById(R.id.token_description);
        descriptionLabel = findViewById(R.id.label_description);
        progressBar = findViewById(R.id.progress);
        tivTokenId = findViewById(R.id.token_id);
        tivNetwork = findViewById(R.id.network);
        tivContractAddress = findViewById(R.id.contract_address);
        tivBalance = findViewById(R.id.balance);
        tivName = findViewById(R.id.name);
        tivExternalLink = findViewById(R.id.external_link);
        tivCreator = findViewById(R.id.creator);
        tivTokenStandard = findViewById(R.id.token_standard);
        tivTotalSupply = findViewById(R.id.total_supply);
        tivNumOwners = findViewById(R.id.num_owners);
        tivOwner = findViewById(R.id.owner);
        tivLastSale = findViewById(R.id.last_sale);
        tivAveragePrice = findViewById(R.id.average_price);
        tivFloorPrice = findViewById(R.id.floor_price);
        tivRarityData = findViewById(R.id.rarity);

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
        viewModel.loadWallet(walletAddress);
        if (C.ACTION_TOKEN_SHORTCUT.equals(getIntent().getAction()))
        {
            handleShortCut(walletAddress);
        }
        else
        {
            token = resolveAssetToken();
            setup();
        }

        viewModel.startGasPriceUpdate(chainId);
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
            return viewModel.getTokensService().getToken(chainId, getIntent().getStringExtra(C.EXTRA_ADDRESS));
        }
    }

    private void handleShortCut(String walletAddress)
    {
        String tokenAddress = getIntent().getStringExtra(C.EXTRA_ADDRESS);
        token = viewModel.getTokensService().getToken(walletAddress, chainId, tokenAddress);
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
        viewModel.checkForNewScript(token);
        viewModel.checkTokenScriptValidity(token);
        setTitle(token.tokenInfo.name);
        updateDefaultTokenData();

        if (asset != null && asset.isAttestation())
        {
            setupAttestation(viewModel.getAssetDefinitionService().getAssetDefinition(token));
        }
        else
        {
            viewModel.getAsset(token, tokenId);
            viewModel.updateLocalAttributes(token, tokenId);
        }
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this)
                .get(TokenFunctionViewModel.class);
        viewModel.gasEstimateComplete().observe(this, this::checkConfirm);
        viewModel.gasEstimateError().observe(this, this::estimateError);
        viewModel.nftAsset().observe(this, this::onNftAsset);
        viewModel.transactionFinalised().observe(this, this::txWritten);
        viewModel.transactionError().observe(this, this::txError);
        //viewModel.scriptUpdateInProgress().observe(this, this::startScriptDownload);
        viewModel.sig().observe(this, this::onSignature);
        //viewModel.newScriptFound().observe(this, this::newScriptFound);
        //viewModel.walletUpdate().observe(this, this::setupFunctionBar);
        //viewModel.attrFetchComplete().observe(this, this::displayTokenView);

        DappBrowserViewModel dappViewModel = new ViewModelProvider(this)
                .get(DappBrowserViewModel.class);

        dappViewModel.defaultWallet().observe(this, this::onDefaultWallet);
        activeNetwork = dappViewModel.getActiveNetwork();

        dappViewModel.findWallet();

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

    private void newScriptFound(TokenDefinition td)
    {
        CertifiedToolbarView certificateToolbar = findViewById(R.id.certified_toolbar);
        //determinate signature
        if (token != null && td.isChanged())
        {
            certificateToolbar.stopDownload();
            certificateToolbar.setVisibility(View.VISIBLE);
            viewModel.checkTokenScriptValidity(token);

            setTitle(token.getTokenName(viewModel.getAssetDefinitionService(), 1));

            //now re-load the verbs if already called. If wallet is null this won't complete
            setupFunctionBar(viewModel.getWallet());

            if (token.getInterfaceSpec() == ContractType.ATTESTATION)
            {
                setupAttestation(td);
            }
            else
            {
                //displayTokenView(td);
            }
        }
        else
        {
            certificateToolbar.stopDownload();
            setupAttestation(null);
        }
    }

    private void onSignature(XMLDsigDescriptor descriptor)
    {
        CertifiedToolbarView certificateToolbar = findViewById(R.id.certified_toolbar);
        certificateToolbar.onSigData(descriptor, this);
    }

    private void startScriptDownload(Boolean status)
    {
        CertifiedToolbarView certificateToolbar = findViewById(R.id.certified_toolbar);
        certificateToolbar.setVisibility(View.VISIBLE);
        if (status)
        {
            certificateToolbar.startDownload();
        }
        else
        {
            certificateToolbar.stopDownload();
            certificateToolbar.hideCertificateResource();
        }
    }

    private void setupFunctionBar(Wallet wallet)
    {
        if (token != null && wallet != null && (BuildConfig.DEBUG || wallet.type != WalletType.WATCH))
        {
            FunctionButtonBar functionBar = findViewById(R.id.layoutButtons);
            functionBar.setupFunctions(this, viewModel.getAssetDefinitionService(), token, null, Collections.singletonList(tokenId));
            functionBar.revealButtons();
            functionBar.setWalletType(wallet.type);
        }
    }

    private void completeAttestationTokenScriptSetup(TSAction action)
    {
        List<Attribute> attestationAttrs = viewModel.getAssetDefinitionService().getAttestationAttrs(token, action, asset.getAttestationID());
        if (attestationAttrs != null)
        {
            for (Attribute attr : attestationAttrs)
            {
                token.setAttributeResult(tokenId, attr);
            }
        }
    }

    private void completeTokenScriptSetup()
    {
        final List<Attribute> attrs = new ArrayList<>();

        if (viewModel.hasTokenScript(token))
        {
            viewModel.getAssetDefinitionService().resolveAttrs(token, new ArrayList<>(Collections.singleton(tokenId)), null)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(attrs::add, this::onError, () -> showTSAttributes(attrs))
                    .isDisposed();
        }
    }

    private void reloadMetadata()
    {
        triggeredReload = true;
        refreshMenu = findViewById(R.id.action_reload_metadata);
        refreshMenu.startAnimation(rotation);
        progressBar.setVisibility(View.VISIBLE);
        viewModel.reloadMetadata(token, tokenId);
    }

    private void clearRefreshAnimation()
    {
        if (refreshMenu != null)
        {
            refreshMenu.clearAnimation();
        }
        progressBar.setVisibility(View.GONE);
    }

    private void onNftAsset(NFTAsset asset)
    {
        if (token != null)
        {
            loadAssetFromMetadata(asset);
        }
    }

    private void updateDefaultTokenData()
    {
        String displayTokenId = "";
        if (!TextUtils.isEmpty(sequenceId))
        {
            displayTokenId = sequenceId;
        }
        else if (tokenId != null)
        {
            displayTokenId = tokenId.toString();
        }

        tivTokenId.setValue(displayTokenId);
        tivTokenId.setCopyableValue(displayTokenId);

        tivNetwork.setValue(token.getNetworkName());

        tivContractAddress.setCopyableValue(token.tokenInfo.address);

        switch (token.getInterfaceSpec())
        {
            case ERC721_LEGACY:
            case ERC721:
            case ERC721_ENUMERABLE:
                tivTokenStandard.setValue(getString(R.string.erc721));
                break;
            case ERC1155:
                tivTokenStandard.setValue(getString(R.string.erc1155));
                break;
            case ATTESTATION:
                tivContractAddress.setVisibility(View.GONE);
                break;
            case ERC721_UNDETERMINED:
            default:
                break;
        }
    }

    private void loadAssetFromMetadata(NFTAsset loadedAsset)
    {
        if (loadedAsset != null)
        {
            updateTokenImage(loadedAsset);

            addMetaDataInfo(loadedAsset);

            nftAttributeLayout.bind(token, loadedAsset);

            clearRefreshAnimation();

            loadFromOpenSeaData(loadedAsset.getOpenSeaAsset());

            completeTokenScriptSetup();
        }
    }

    private void showTSAttributes(List<Attribute> attrs)
    {
        //should have resolved all the attrs
        tsAttributeLayout.bindTSAttributes(attrs);
    }

    private void onError(Throwable throwable)
    {
        Timber.w(throwable);
    }

    private void updateTokenImage(NFTAsset asset)
    {
        if (triggeredReload) tokenImage.clearImage();
        tokenImage.setupTokenImage(asset);
        triggeredReload = false;

        if (!tokenImage.isDisplayingImage() && TextUtils.isEmpty(asset.getImage()))
        {
            tokenImage.showFallbackLayout(token);
        }
    }

    private void addMetaDataInfo(NFTAsset asset)
    {
        updateDefaultTokenData();

        if (asset.isAssetMultiple())
        {
            tivBalance.setValue(asset.getBalance().toString());
        }

        String assetName = asset.getName();
        if (assetName != null)
        {
            tivName.setValue(assetName);
            setTitle(assetName);
        }

        tivExternalLink.setValue(asset.getExternalLink());

        updateDescription(asset.getDescription());

        nftAttributeLayout.bind(token, asset);
    }

    private void updateDescription(String description)
    {
        if (!TextUtils.isEmpty(description))
        {
            descriptionLabel.setVisibility(View.VISIBLE);
            tokenDescription.setText(Html.fromHtml(description, FROM_HTML_MODE_LEGACY));
        }
    }

    private void loadFromOpenSeaData(OpenSeaAsset openSeaAsset)
    {
        if (openSeaAsset == null) return;

        updateDefaultTokenData();

        String name = openSeaAsset.name;
        if (!TextUtils.isEmpty(name))
        {
            setTitle(name);
            tivName.setValue(name);
        }

        if (openSeaAsset.creator != null
                && openSeaAsset.creator.user != null)
        {
            tivCreator.setValue(openSeaAsset.creator.user.username);
        }

        if (openSeaAsset.assetContract != null)
        {
            tivTokenStandard.setValue(openSeaAsset.assetContract.getSchemaName());
        }

        if (openSeaAsset.collection != null
                && openSeaAsset.collection.stats != null)
        {
            tivTotalSupply.setValue(String.valueOf(openSeaAsset.collection.stats.totalSupply));
            tivNumOwners.setValue(String.valueOf(openSeaAsset.collection.stats.numOwners));
            nftAttributeLayout.bind(token, openSeaAsset.traits, openSeaAsset.collection.stats.count);
        }
        else
        {
            nftAttributeLayout.bind(token, openSeaAsset.traits, 0);
        }

        if (openSeaAsset.rarity != null && openSeaAsset.rarity.rank > 0)
        {
            tivRarityData.setValue("#" + openSeaAsset.rarity.rank);
        }

        if (openSeaAsset.owner != null
                && openSeaAsset.owner.user != null)
        {
            tivOwner.setValue(openSeaAsset.owner.user.username);
        }

        tivLastSale.setValue(openSeaAsset.getLastSale());

        tivAveragePrice.setValue(openSeaAsset.getAveragePrice());

        tivFloorPrice.setValue(openSeaAsset.getFloorPrice());

        tivExternalLink.setValue(openSeaAsset.externalLink);

        updateDescription(openSeaAsset.description);

        clearRefreshAnimation();
    }

    private void setupAttestation(TokenDefinition td)
    {
        NFTAsset attnAsset = new NFTAsset();
        if (token.getInterfaceSpec() != ContractType.ATTESTATION)
        {
            return;
        }
        else if (td != null)
        {
            attnAsset.setupScriptElements(td);
            attnAsset.setupScriptAttributes(td, token);
            //if (!displayTokenView(td))
            //{
                tokenImage.setupTokenImage(attnAsset);
            //}*/
            setTitle(attnAsset.getName());
            if (!TextUtils.isEmpty(attnAsset.getDescription()))
            {
                tokenDescription.setVisibility(View.VISIBLE);
                tokenDescription.setText(attnAsset.getDescription());
            }
        }
        else
        {
            tokenImage.setAttestationImage(token);
            token.addAssetElements(attnAsset, this);
            tokenDescription.setVisibility(View.GONE);
        }

        progressBar.setVisibility(View.GONE);
        tivTokenId.setVisibility(View.GONE);
        showIssuer(((Attestation)token).getIssuer());

        //now populate
        nftAttributeLayout.bind(token, attnAsset);
    }

    /**
     * Final return path
     * @param transactionReturn write success hash back to ActionSheet
     */
    private void txWritten(TransactionReturn transactionReturn)
    {
        confirmationDialog.transactionWritten(transactionReturn.hash); //display hash and success in ActionSheet, start 1 second timer to dismiss.
    }

    //Transaction failed to be sent
    private void txError(TransactionReturn txReturn)
    {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(ERROR);
        dialog.setTitle(R.string.error_transaction_failed);
        dialog.setMessage(txReturn.throwable.getMessage());
        dialog.setButtonText(R.string.button_ok);
        dialog.setButtonListener(v -> {
            dialog.dismiss();
        });
        dialog.show();
        confirmationDialog.dismiss();
    }

    @Override
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
    }

    @Override
    public void handleTokenScriptFunction(String function, List<BigInteger> selection)
    {
        //does the function have a view? If it's transaction only then handle here
        Map<String, TSAction> functions = viewModel.getAssetDefinitionService().getTokenFunctionMap(token);
        if (functions == null) return;
        TSAction action = functions.get(function);
        token.clearResultMap();

        BigInteger tokenId = selection.size() > 0 ? selection.get(0) : BigInteger.ONE;

        //handle TS function
        if (action != null && action.view == null && action.function != null)
        {
            //test if we need to build attribute the list
            completeAttestationTokenScriptSetup(action);
            //viewModel.loadAttributesIfRequired();

            //open action sheet after we determine the gas limit
            Web3Transaction web3Tx = viewModel.handleFunction(action, tokenId, token, this);
            if (web3Tx.gasLimit.equals(BigInteger.ZERO))
            {
                calculateEstimateDialog();
                //get gas estimate
                viewModel.estimateGasLimit(web3Tx, token.tokenInfo.chainId);
            }
            else
            {
                //go straight to confirmation
                checkConfirm(web3Tx);
            }
        }
        else
        {
            viewModel.showFunction(this, token, function, selection, asset);
        }
    }

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

    private void showIssuer(String issuer)
    {
        if (!TextUtils.isEmpty(issuer))
        {
            ((TokenInfoView)findViewById(R.id.key_address)).setCopyableValue(issuer);
            ((TokenInfoView)findViewById(R.id.key_address)).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void getAuthorisation(SignAuthenticationCallback callback)
    {
        viewModel.getAuthentication(this, callback);
    }

    @Override
    public void sendTransaction(Web3Transaction tx)
    {
        viewModel.requestSignature(tx, viewModel.getWallet(), token.tokenInfo.chainId);
    }

    @Override
    public void completeSendTransaction(Web3Transaction tx, SignatureFromKey signature)
    {
        viewModel.sendTransaction(viewModel.getWallet(), token.tokenInfo.chainId, tx, signature);
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
        viewModel.actionSheetConfirm(mode);
    }

    @Override
    public ActivityResultLauncher<Intent> gasSelectLauncher()
    {
        return getGasSettings;
    }

    @Override
    public WalletType getWalletType()
    {
        return viewModel.getWallet().type;
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

            //tokenScriptView.resetView();
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
        /*final NetworkInfo info = viewModel.getNetworkInfo(chainId);

        if (forceChainChange != 0 || getContext() == null)
        {
            return; //No action if chain change is forced
        }

        // handle unknown network
        if (info == null)
        {
            // show add custom chain dialog
            addCustomChainDialog = new AddEthereumChainPrompt(getContext(), chainObj, chainObject -> {
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
        }*/
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

        DappBrowserViewModel dappViewModel = new ViewModelProvider(this)
                .get(DappBrowserViewModel.class);

        final NetworkInfo info = dappViewModel.getNetworkInfo(chainId);

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
                confirmationDialog.setURL(url);
                confirmationDialog.setCanceledOnTouchOutside(false);
                confirmationDialog.show();
                confirmationDialog.fullExpand();

                DappBrowserViewModel dappViewModel = new ViewModelProvider(this)
                        .get(DappBrowserViewModel.class);

                dappViewModel.calculateGasEstimate(wallet, transaction, activeNetwork.chainId)
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

}
