package com.alphawallet.app.ui;

import static com.alphawallet.app.widget.AWalletAlertDialog.WARNING;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
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
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.ui.widget.entity.ActionSheetCallback;
import com.alphawallet.app.ui.widget.entity.NFTAttributeLayout;
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
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


@AndroidEntryPoint
public class NFTAssetDetailActivity extends BaseActivity implements StandardFunctionInterface, ActionSheetCallback
{
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

    private final ActivityResultLauncher<Intent> handleTransactionSuccess = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nft_asset_detail);

        toolbar();

        initViewModel();

        getIntentData();

        setTitle(token.tokenInfo.name);

        initViews();

        setupFunctionBar();

        asset = token.getTokenAssets().get(tokenId);

        loadAssetData(asset);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (viewModel != null) viewModel.prepare();
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

        rotation = AnimationUtils.loadAnimation(this, R.anim.rotate_refresh);
        rotation.setRepeatCount(Animation.INFINITE);
    }

    private void reloadMetadata()
    {
        refreshMenu = findViewById(R.id.action_reload_metadata);
        refreshMenu.startAnimation(rotation);
        fetchAsset(tokenId, asset);
    }

    private void fetchAsset(BigInteger tokenId, NFTAsset nftAsset)
    {
        nftAsset.metaDataLoader =
                Single.fromCallable(() -> token.fetchTokenMetadata(tokenId))
                        .map(newAsset -> storeAsset(tokenId, newAsset, nftAsset))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(asset -> loadAssetData(asset), e -> {
                        });
    }

    private NFTAsset storeAsset(BigInteger tokenId, NFTAsset fetchedAsset, NFTAsset oldAsset)
    {
        fetchedAsset.updateFromRaw(oldAsset);
        viewModel.getTokensService().storeAsset(token, tokenId, fetchedAsset);
        token.addAssetToTokenBalanceAssets(tokenId, fetchedAsset);
        return fetchedAsset;
    }

    private void loadAssetData(NFTAsset asset)
    {
        if (asset == null) return;

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

        tokenInfoLayout.removeAllViews();
        nftAttributeLayout.removeAllViews();

        tokenInfoLayout.addView(new TokenInfoCategoryView(this, getString(R.string.label_details)));

        //can be either: FT with a balance (balance > 1)
        //unique NFT with tokenId (sequenceId)
        //TODO: This should be done in a common widget together with all other instances
        if (!TextUtils.isEmpty(sequenceId))
        {
            addInfoView(getString(R.string.label_token_id), sequenceId);
        }
        else
        {
            addInfoView(getString(R.string.label_token_id), tokenId.toString());
        }
        if (asset.isAssetMultiple())
        {
            addInfoView(getString(R.string.balance), asset.getBalance().toString());
        }
        if (!TextUtils.isEmpty(asset.getName()))
        {
            addInfoView(getString(R.string.hint_contract_name), asset.getName());
        }
        addInfoView(getString(R.string.label_external_link), asset.getExternalLink());

        nftAttributeLayout.bind(token, asset);

        String description = asset.getDescription();
        if (description != null)
        {
            tokenInfoLayout.addView(new TokenInfoCategoryView(this, getString(R.string.label_description)));
            tokenDescription.setText(asset.getDescription());
        }

        tokenInfoLayout.forceLayout();

        if (refreshMenu != null)
        {
            refreshMenu.clearAnimation();
        }
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

    private void addInfoView(String elementName, String name)
    {
        if (!TextUtils.isEmpty(name))
        {
            TokenInfoView v = new TokenInfoView(this, elementName);
            v.setValue(name);
            tokenInfoLayout.addView(v);
        }
    }

    @Override
    public void showTransferToken(List<BigInteger> selection)
    {
        NFTAsset asset = token.getTokenAssets().get(tokenId);
        if (asset.isAssetMultiple())
        {
            viewModel.showTransferSelectCount(this, token, tokenId)
                    .subscribe(intent -> handleTransactionSuccess.launch(intent)).isDisposed();
        }
        else
        {
            if (asset.getSelectedBalance().compareTo(BigDecimal.ZERO) == 0)
            {
                asset.setSelectedBalance(BigDecimal.ONE);
            }
            viewModel.getTransferIntent(this, token, Collections.singletonList(tokenId), new ArrayList<>(Collections.singletonList(asset)))
                    .subscribe(intent -> handleTransactionSuccess.launch(intent)).isDisposed();
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
        dialog.setButtonListener(v -> {
            BigInteger gasEstimate = GasService.getDefaultGasLimit(token, w3tx);
            checkConfirm(new Web3Transaction(w3tx.recipient, w3tx.contract, w3tx.value, w3tx.gasPrice, gasEstimate, w3tx.nonce, w3tx.payload, w3tx.description));
        });

        dialog.setSecondaryButtonListener(v -> {
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
}
