package com.alphawallet.app.ui;

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
import android.widget.LinearLayout;
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
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.opensea.OpenSeaAsset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.ui.widget.entity.NFTAttributeLayout;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.TokenFunctionViewModel;
import com.alphawallet.app.web3.entity.Web3Transaction;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.ActionSheetDialog;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.app.widget.NFTImageView;
import com.alphawallet.app.widget.TokenInfoCategoryView;
import com.alphawallet.app.widget.TokenInfoView;
import com.alphawallet.ethereum.EthereumNetworkBase;
import com.alphawallet.token.entity.TSAction;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.functions.Consumer;


@AndroidEntryPoint
public class NFTAssetDetailActivity extends BaseActivity implements StandardFunctionInterface, ActionSheetCallback {
    private TokenFunctionViewModel viewModel;
    private Token token;
    private Wallet wallet;
    private BigInteger tokenId;
    private String sequenceId;
    private LinearLayout tokenInfoLayout;
    private ActionSheetDialog confirmationDialog;
    private AWalletAlertDialog dialog;
    private NFTAsset asset;
    private NFTImageView tokenImage;
    private NFTAttributeLayout nftAttributeLayout;
    private TextView tokenDescription;
    private ActionMenuItemView refreshMenu;
    private Animation rotation;
    private TokenInfoCategoryView detailsLabel;
    private TokenInfoCategoryView descriptionLabel;
    private ProgressBar progressBar;
    private ActivityResultLauncher<Intent> handleTransactionSuccess;
    private ActivityResultLauncher<Intent> getGasSettings;


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

        asset = token.getTokenAssets().get(tokenId);

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
        tokenInfoLayout = findViewById(R.id.layout_token_info);
        tokenImage = findViewById(R.id.asset_image);
        nftAttributeLayout = findViewById(R.id.attributes);
        tokenDescription = findViewById(R.id.token_description);
        detailsLabel = findViewById(R.id.label_details);
        descriptionLabel = findViewById(R.id.label_description);
        progressBar = findViewById(R.id.progress);

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
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this)
                .get(TokenFunctionViewModel.class);
        viewModel.gasEstimateComplete().observe(this, this::checkConfirm);
        viewModel.openSeaAsset().observe(this, this::onOpenSeaAsset);
        viewModel.nftAsset().observe(this, this::onNftAsset);
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
        refreshMenu = findViewById(R.id.action_reload_metadata);
        refreshMenu.startAnimation(rotation);
        progressBar.setVisibility(View.VISIBLE);
        viewModel.getAsset(token, tokenId);
    }

    private void clearRefreshAnimation()
    {
        if (refreshMenu != null)
        {
            refreshMenu.clearAnimation();
        }
        progressBar.setVisibility(View.GONE);
    }

    private void addInfoView(String elementName, String name)
    {
        if (!TextUtils.isEmpty(name))
        {
            TokenInfoView v = new TokenInfoView(this, elementName);
            v.setValue(name);
            tokenInfoLayout.addView(v);
        }
    }

    private void onNftAsset(NFTAsset asset)
    {
        loadAssetFromMetadata(asset);
    }

    private void updateDefaultTokenData()
    {
        tokenInfoLayout.removeAllViews();

        if (!TextUtils.isEmpty(sequenceId))
        {
            addInfoView(getString(R.string.label_token_id), sequenceId);
        }
        else
        {
            addInfoView(getString(R.string.label_token_id), tokenId.toString());
        }

        addInfoView(getString(R.string.subtitle_network), token.getNetworkName());

        addInfoView(getString(R.string.contract_address), Utils.formatAddress(token.tokenInfo.address));
    }

    private void loadAssetFromMetadata(NFTAsset asset)
    {
        if (asset != null)
        {
            updateTokenImage(asset);

            addMetaDataInfo(asset);

            nftAttributeLayout.bind(token, asset);

            clearRefreshAnimation();
        }
    }

    private void updateTokenImage(NFTAsset asset)
    {
        if (asset.isBlank())
        {
            tokenImage.showFallbackLayout(token);
        }
        else
        {
            tokenImage.setWebViewHeight(tokenImage.getLayoutParams().width);
            tokenImage.showLoadingProgress(true);
            tokenImage.setupTokenImage(asset);
        }
    }

    private void updateTokenImage(OpenSeaAsset openSeaAsset)
    {
        if (TextUtils.isEmpty(openSeaAsset.getImageUrl()))
        {
            tokenImage.showFallbackLayout(token);
        }
        else
        {
            tokenImage.setWebViewHeight(tokenImage.getLayoutParams().width);
            tokenImage.showLoadingProgress(true);
            tokenImage.setupTokenImage(openSeaAsset);
        }
    }

    private void addMetaDataInfo(NFTAsset asset)
    {
        updateDefaultTokenData();

        if (asset.isAssetMultiple())
        {
            addInfoView(getString(R.string.balance), asset.getBalance().toString());
        }

        String assetName = asset.getName();
        if (assetName != null)
        {
            addInfoView(getString(R.string.hint_contract_name), assetName);
            setTitle(assetName);
        }

        addInfoView(getString(R.string.label_external_link), asset.getExternalLink());

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
        updateDefaultTokenData();

        updateTokenImage(openSeaAsset);

        if (!TextUtils.isEmpty(openSeaAsset.name))
        {
            setTitle(openSeaAsset.name);
            addInfoView(getString(R.string.hint_contract_name), openSeaAsset.name);
        }

        if (openSeaAsset.creator != null
                && openSeaAsset.creator.user != null)
        {
            addInfoView(getString(R.string.asset_creator), openSeaAsset.creator.user.username);
        }

        if (openSeaAsset.assetContract != null)
        {
            addInfoView(getString(R.string.asset_schema), openSeaAsset.assetContract.getSchemaName());
        }

        if (openSeaAsset.collection != null
                && openSeaAsset.collection.stats != null)
        {
            addInfoView(getString(R.string.asset_total_supply), String.valueOf(openSeaAsset.collection.stats.totalSupply));
            addInfoView(getString(R.string.asset_number_of_owners), String.valueOf(openSeaAsset.collection.stats.numOwners));
            nftAttributeLayout.bind(token, openSeaAsset.traits, openSeaAsset.collection.stats.count);
        }
        else
        {
            nftAttributeLayout.bind(token, openSeaAsset.traits, 0);
        }

        if (openSeaAsset.owner != null
                && openSeaAsset.owner.user != null)
        {
            addInfoView(getString(R.string.asset_owner), openSeaAsset.owner.user.username);
        }

        if (openSeaAsset.lastSale != null
                && openSeaAsset.lastSale.paymentToken != null)
        {
            String salePrice = openSeaAsset.lastSale.totalPrice;
            int endIndex = salePrice.length() - openSeaAsset.lastSale.paymentToken.decimals;
            String result = salePrice.substring(0, endIndex)
                    + " " + openSeaAsset.lastSale.paymentToken.symbol;
            addInfoView(getString(R.string.asset_last_sale), result);
        }

        addInfoView(getString(R.string.label_external_link), openSeaAsset.externalLink);

        updateDescription(openSeaAsset.description);

        clearRefreshAnimation();
    }

    private void onOpenSeaAsset(OpenSeaAsset openSeaAsset)
    {
        loadFromOpenSeaData(openSeaAsset);
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
