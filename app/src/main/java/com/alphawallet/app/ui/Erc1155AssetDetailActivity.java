package com.alphawallet.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.ui.widget.entity.NFTAttributeLayout;
import com.alphawallet.app.viewmodel.Erc1155AssetDetailViewModel;
import com.alphawallet.app.viewmodel.Erc1155AssetDetailViewModelFactory;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.app.widget.NFTImageView;
import com.alphawallet.app.widget.TokenInfoCategoryView;
import com.alphawallet.app.widget.TokenInfoView;
import com.alphawallet.ethereum.EthereumNetworkBase;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class Erc1155AssetDetailActivity extends BaseActivity implements StandardFunctionInterface {
    @Inject
    Erc1155AssetDetailViewModelFactory viewModelFactory;
    Erc1155AssetDetailViewModel viewModel;

    private Token token;
    private Wallet wallet;
    private BigInteger tokenId;
    private String sequenceId;

    private LinearLayout tokenInfoLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_erc1155_asset_detail);

        toolbar();

        initViewModel();

        getIntentData();

        setTitle(token.tokenInfo.name);

        initViews();

        setupFunctionBar();
    }

    private void initViews()
    {
        tokenInfoLayout = findViewById(R.id.layout_token_info);
        NFTImageView tokenImage = findViewById(R.id.asset_image);
        NFTAttributeLayout attrs = findViewById(R.id.attributes);
        NFTAsset asset = token.getTokenAssets().get(tokenId);

        if (asset == null) return;

        tokenImage.setupTokenImage(asset);

        TextView tokenDescription = findViewById(R.id.token_description);

        tokenInfoLayout.addView(new TokenInfoCategoryView(this, "Details"));

        //can be either: FT with a balance (balance > 1)
        //unique NFT with tokenId (sequenceId)

        if (!TextUtils.isEmpty(sequenceId)) { addInfoView("Token #", sequenceId); }
        if (asset.isAssetMultiple()) { addInfoView(getString(R.string.balance), asset.getBalance().toString()); }
        if (!TextUtils.isEmpty(asset.getName())) { addInfoView(getString(R.string.hint_contract_name), asset.getName()); }
        addInfoView("External Link", asset.getExternalLink());
        tokenInfoLayout.addView(new TokenInfoCategoryView(this, "Description"));
        attrs.bind(token, asset);
        tokenDescription.setText(asset.getDescription());

        tokenInfoLayout.forceLayout();
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
        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(Erc1155AssetDetailViewModel.class);
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

    ActivityResultLauncher<Intent> handleTransactionSuccess = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
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
            if (asset.getSelectedBalance().compareTo(BigDecimal.ZERO) == 0) { asset.setSelectedBalance(BigDecimal.ONE); }
            viewModel.getTransferIntent(this, token, Collections.singletonList(tokenId), new ArrayList<>(Collections.singletonList(asset)))
                    .subscribe(intent -> handleTransactionSuccess.launch(intent)).isDisposed();
        }
    }
}
