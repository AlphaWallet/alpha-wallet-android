package com.alphawallet.app.ui;

import static android.text.Html.FROM_HTML_MODE_LEGACY;
import static com.alphawallet.app.widget.AWalletAlertDialog.ERROR;
import static com.alphawallet.app.widget.AWalletAlertDialog.WARNING;
import static java.util.Collections.singletonList;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.GasEstimate;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.TransactionReturn;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.opensea.OpenSeaAsset;
import com.alphawallet.app.entity.tokens.Attestation;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.ui.widget.entity.NFTAttributeLayout;
import com.alphawallet.app.util.ShortcutUtils;
import com.alphawallet.app.viewmodel.TokenFunctionViewModel;
import com.alphawallet.app.web3.Web3TokenView;
import com.alphawallet.app.web3.entity.Address;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.ActionSheetDialog;
import com.alphawallet.app.widget.CertifiedToolbarView;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.app.widget.NFTImageView;
import com.alphawallet.app.widget.TokenInfoCategoryView;
import com.alphawallet.app.widget.TokenInfoView;
import com.alphawallet.ethereum.EthereumNetworkBase;
import com.alphawallet.hardware.SignatureFromKey;
import com.alphawallet.token.entity.TSAction;
import com.alphawallet.token.entity.TicketRange;
import com.alphawallet.token.entity.TokenScriptResult;
import com.alphawallet.token.entity.ViewType;
import com.alphawallet.token.entity.XMLDsigDescriptor;
import com.alphawallet.token.tools.TokenDefinition;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.functions.Consumer;
import timber.log.Timber;

@AndroidEntryPoint
public class NFTAssetDetailActivity extends BaseActivity implements StandardFunctionInterface, ActionSheetCallback
{
    private TokenFunctionViewModel viewModel;
    private Token token;
    private BigInteger tokenId;
    private NFTAsset asset;
    private String sequenceId;
    private ActionSheetDialog confirmationDialog;
    private AWalletAlertDialog dialog;
    private NFTImageView tokenImage;
    private NFTAttributeLayout nftAttributeLayout;
    private NFTAttributeLayout tsAttributeLayout;
    private TextView tokenDescription;
    @SuppressLint("RestrictedApi")
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
    private Web3TokenView tokenScriptView;
    private boolean usingNativeTokenScript = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nft_asset_detail);

        initViews();

        toolbar();

        initIntents();

        initViewModel();

        usingNativeTokenScript = !viewModel.getUseTSViewer();
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
        tsAttributeLayout = findViewById(R.id.ts_attributes);
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
            ShortcutUtils.showConfirmationDialog(this, singletonList(tokenAddress), getString(R.string.remove_shortcut_while_token_not_found), null);
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
            if (this.usingNativeTokenScript)
            {
                viewModel.updateLocalAttributes(token, tokenId);  //when complete calls displayTokenView
            }
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
        viewModel.scriptUpdateInProgress().observe(this, this::startScriptDownload);
        viewModel.sig().observe(this, this::onSignature);
        viewModel.newScriptFound().observe(this, this::newScriptFound);
        viewModel.walletUpdate().observe(this, this::setupFunctionBar);
        viewModel.attrFetchComplete().observe(this, this::displayTokenView); //local attr fetch
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
            else if (this.usingNativeTokenScript)
            {
                displayTokenView(td);
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

            if (this.usingNativeTokenScript)
            {
                functionBar.setupFunctions(this, viewModel.getAssetDefinitionService(), token, null, Collections.singletonList(tokenId));
            }
            else
            {
                functionBar.setupFunctionsForJsViewer(this, R.string.title_tokenscript, this.token, Collections.singletonList(tokenId));
            }

            functionBar.revealButtons();
            functionBar.setWalletType(wallet.type);
        }
    }

    private void completeAttestationTokenScriptSetup(TSAction action)
    {
        List<TokenScriptResult.Attribute> attestationAttrs = viewModel.getAssetDefinitionService().getAttestationAttrs(token, action, asset.getAttestationID());
        if (attestationAttrs != null)
        {
            for (TokenScriptResult.Attribute attr : attestationAttrs)
            {
                token.setAttributeResult(tokenId, attr);
            }
        }
    }

    private void completeTokenScriptSetup(String prevResult)
    {
        viewModel.completeTokenScriptSetup(token, tokenId, prevResult, (attrs, needsUpdate) -> {
            //should have resolved all the attrs
            tsAttributeLayout.bindTSAttributes(attrs);
            //require refresh of TS View
            if (needsUpdate)
            {
                displayTokenView(viewModel.getAssetDefinitionService().getAssetDefinition(token));
            }
        });
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
        }
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
        if (token == null || token.getInterfaceSpec() != ContractType.ATTESTATION)
        {
            return;
        }
        else if (td != null)
        {
            attnAsset.setupScriptElements(td);
            attnAsset.setupScriptAttributes(td, token);
            if (!displayTokenView(td)) //display token for Attribute
            {
                tokenImage.setupTokenImage(attnAsset);
            }
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
            findViewById(R.id.key_address).setVisibility(View.VISIBLE);
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
        if (actionCompleted)
        {
            Intent intent = new Intent();
            intent.putExtra(C.EXTRA_TXHASH, txHash);
            setResult(RESULT_OK, intent);
            finish();
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

    @Override
    public GasService getGasService()
    {
        return viewModel.getGasService();
    }

    @Override
    public void completeFunctionSetup()
    {
        //check if TS needs to be refreshed
        completeTokenScriptSetup(tokenScriptView.getAttrResults());
    }

    /***
     * TokenScript view handling
     */
    private boolean displayTokenView(final TokenDefinition td)
    {
        boolean couldDisplay = false;
        try
        {
            LinearLayout webWrapper = findViewById(R.id.layout_webwrapper);
            //restart if required
            if (tokenScriptView != null)
            {
                webWrapper.removeView(tokenScriptView);
                tokenScriptView.destroy();
                tokenScriptView = null;
            }

            tokenScriptView = new Web3TokenView(this);
            tokenScriptView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            tokenScriptView.clearCache(true);

            if (tokenScriptView.renderTokenScriptInfoView(token, new TicketRange(tokenId, token.getAddress()), viewModel.getAssetDefinitionService(), ViewType.VIEW, td))
            {
                webWrapper.setVisibility(View.VISIBLE);
                tokenScriptView.setChainId(token.tokenInfo.chainId);
                tokenScriptView.setWalletAddress(new Address(token.getWallet()));
                webWrapper.addView(tokenScriptView);
                couldDisplay = true;
            }
        }
        catch (Exception e)
        {
            Timber.e(e);
            //fillEmpty();
        }

        return couldDisplay;
    }

    public void handleClick(String action, int actionId) {

        if (actionId != R.string.title_tokenscript)
            return;

        Intent intent = new Intent(NFTAssetDetailActivity.this, TokenScriptJsActivity.class);
        intent.putExtra(C.Key.WALLET, (Wallet) getIntent().getParcelableExtra(C.Key.WALLET));
        intent.putExtra(C.EXTRA_CHAIN_ID, getIntent().getLongExtra(C.EXTRA_CHAIN_ID, chainId));
        intent.putExtra(C.EXTRA_ADDRESS, getIntent().getStringExtra(C.EXTRA_ADDRESS));
        intent.putExtra(C.EXTRA_TOKEN_ID, getIntent().getStringExtra(C.EXTRA_TOKEN_ID));
        if (asset != null) intent.putExtra(C.EXTRA_NFTASSET, (NFTAsset) getIntent().getParcelableExtra(C.EXTRA_NFTASSET));
        startActivity(intent);
    }
}
