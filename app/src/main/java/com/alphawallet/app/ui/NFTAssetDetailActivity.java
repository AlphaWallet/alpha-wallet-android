package com.alphawallet.app.ui;

import static com.alphawallet.app.widget.AWalletAlertDialog.ERROR;
import static com.alphawallet.app.widget.AWalletAlertDialog.WARNING;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.TransactionData;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.opensea.OpenSeaAsset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.ui.widget.entity.NFTAttributeLayout;
import com.alphawallet.app.viewmodel.TokenFunctionViewModel;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.ActionSheetDialog;
import com.alphawallet.app.widget.CertifiedToolbarView;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.app.widget.NFTImageView;
import com.alphawallet.app.widget.TokenInfoCategoryView;
import com.alphawallet.app.widget.TokenInfoView;
import com.alphawallet.ethereum.EthereumNetworkBase;
import com.alphawallet.token.entity.TSAction;
import com.alphawallet.token.entity.XMLDsigDescriptor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.functions.Consumer;


@AndroidEntryPoint
public class NFTAssetDetailActivity extends BaseActivity implements StandardFunctionInterface, ActionSheetCallback
{
    private TokenFunctionViewModel viewModel;
    private Token token;
    private Wallet wallet;
    private BigInteger tokenId;
    private String sequenceId;
    private ActionSheetDialog confirmationDialog;
    private AWalletAlertDialog dialog;
    private NFTImageView tokenImage;
    private NFTAttributeLayout nftAttributeLayout;
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nft_asset_detail);

        initViews();

        toolbar();

        initIntents();

        initViewModel();

        getIntentData();

        setTitle(token.tokenInfo.name);

        setupFunctionBar();

        updateDefaultTokenData();
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
            viewModel.prepare();
            viewModel.getAsset(token, tokenId);
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy()
    {
        viewModel.onDestroy();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_refresh, menu);
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
        long chainId = getIntent().getLongExtra(C.EXTRA_CHAIN_ID, EthereumNetworkBase.MAINNET_ID);
        token = viewModel.getTokensService().getToken(chainId, getIntent().getStringExtra(C.EXTRA_ADDRESS));
        wallet = getIntent().getParcelableExtra(C.Key.WALLET);
        tokenId = new BigInteger(getIntent().getStringExtra(C.EXTRA_TOKEN_ID));
        sequenceId = getIntent().getStringExtra(C.EXTRA_STATE);
        viewModel.checkForNewScript(token);
        viewModel.checkTokenScriptValidity(token);
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this)
                .get(TokenFunctionViewModel.class);
        viewModel.gasEstimateComplete().observe(this, this::checkConfirm);
        viewModel.nftAsset().observe(this, this::onNftAsset);
        viewModel.transactionFinalised().observe(this, this::txWritten);
        viewModel.transactionError().observe(this, this::txError);
        viewModel.scriptUpdateInProgress().observe(this, this::startScriptDownload);
        viewModel.sig().observe(this, this::onSignature);
        viewModel.newScriptFound().observe(this, this::newScriptFound);
    }

    private void newScriptFound(Boolean status)
    {
        CertifiedToolbarView certificateToolbar = findViewById(R.id.certified_toolbar);
        certificateToolbar.stopDownload();
        //determinate signature
        if (token != null)
        {
            certificateToolbar.setVisibility(View.VISIBLE);
            viewModel.checkTokenScriptValidity(token);

            //now re-load the verbs
            setupFunctionBar();
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

    private void setupFunctionBar()
    {
        if (BuildConfig.DEBUG || wallet.type != WalletType.WATCH)
        {
            FunctionButtonBar functionBar = findViewById(R.id.layoutButtons);
            functionBar.setupFunctions(this, viewModel.getAssetDefinitionService(), token, null, Collections.singletonList(tokenId));
            functionBar.revealButtons();
            functionBar.setWalletType(wallet.type);
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
        loadAssetFromMetadata(asset);
    }

    private void updateDefaultTokenData()
    {
        if (!TextUtils.isEmpty(sequenceId))
        {
            tivTokenId.setValue(sequenceId);
        }
        else
        {
            tivTokenId.setValue(tokenId.toString());
        }

        tivNetwork.setValue(token.getNetworkName());

        tivContractAddress.setCopyableValue(token.tokenInfo.address);
    }

    private void loadAssetFromMetadata(NFTAsset asset)
    {
        if (asset != null)
        {
            updateTokenImage(asset);

            addMetaDataInfo(asset);

            nftAttributeLayout.bind(token, asset);

            clearRefreshAnimation();

            loadFromOpenSeaData(asset.getOpenSeaAsset());
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
            tokenDescription.setText(Html.fromHtml(description));
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

    private void onOpenSeaAsset(OpenSeaAsset openSeaAsset)
    {
        loadFromOpenSeaData(openSeaAsset);
    }

    /**
     * Final return path
     * @param transactionData write success hash back to ActionSheet
     */
    private void txWritten(TransactionData transactionData)
    {
        confirmationDialog.transactionWritten(transactionData.txHash); //display hash and success in ActionSheet, start 1 second timer to dismiss.
    }

    //Transaction failed to be sent
    private void txError(Throwable throwable)
    {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(ERROR);
        dialog.setTitle(R.string.error_transaction_failed);
        dialog.setMessage(throwable.getMessage());
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
                viewModel.getTransferIntent(this, token, Collections.singletonList(tokenId), new ArrayList<>(Collections.singletonList(asset)))
                        .subscribe((Consumer<Intent>) handleTransactionSuccess::launch).isDisposed();
            }
        }
    }

    @Override
    public void handleTokenScriptFunction(String function, List<BigInteger> selection)
    {
        //does the function have a view? If it's transaction only then handle here
        Map<String, TSAction> functions = viewModel.getAssetDefinitionService().getTokenFunctionMap(token.tokenInfo.chainId, token.getAddress());
        if (functions == null) return;
        TSAction action = functions.get(function);
        token.clearResultMap();

        //handle TS function
        if (action != null && action.view == null && action.function != null)
        {
            //open action sheet after we determine the gas limit
            Web3Transaction web3Tx = viewModel.handleFunction(action, selection.get(0), token, this);
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
            viewModel.showFunction(this, token, function, selection);
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

    private void estimateError(final Web3Transaction w3tx)
    {
        if (dialog != null && dialog.isShowing()) dialog.dismiss();
        dialog = new AWalletAlertDialog(this);
        dialog.setIcon(WARNING);
        dialog.setTitle(R.string.confirm_transaction);
        dialog.setMessage(R.string.error_transaction_may_fail);
        dialog.setButtonText(R.string.button_ok);
        dialog.setSecondaryButtonText(R.string.action_cancel);
        dialog.setButtonListener(v ->
        {
            BigInteger gasEstimate = GasService.getDefaultGasLimit(token, w3tx);
            checkConfirm(new Web3Transaction(w3tx.recipient, w3tx.contract, w3tx.value, w3tx.gasPrice, gasEstimate, w3tx.nonce, w3tx.payload, w3tx.description));
        });

        dialog.setSecondaryButtonListener(v ->
        {
            dialog.dismiss();
        });

        dialog.show();
    }

    private void checkConfirm(Web3Transaction w3tx)
    {
        if (w3tx.gasLimit.equals(BigInteger.ZERO))
        {
            estimateError(w3tx);
        }
        else
        {
            if (dialog != null && dialog.isShowing()) dialog.dismiss();
            confirmationDialog = new ActionSheetDialog(this, w3tx, token, "", //TODO: Reverse resolve address
                    w3tx.recipient.toString(), viewModel.getTokenService(), this);
            confirmationDialog.setURL("TokenScript");
            confirmationDialog.setCanceledOnTouchOutside(false);
            confirmationDialog.show();
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
        viewModel.sendTransaction(tx, token.tokenInfo.chainId, ""); //return point is txWritten
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
}
