package com.alphawallet.app.ui;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.viewmodel.Erc1155AssetDetailViewModel;
import com.alphawallet.app.viewmodel.Erc1155AssetDetailViewModelFactory;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.app.widget.TokenInfoCategoryView;
import com.alphawallet.app.widget.TokenInfoView;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class Erc1155AssetDetailActivity extends BaseActivity implements StandardFunctionInterface {
    @Inject
    Erc1155AssetDetailViewModelFactory viewModelFactory;
    Erc1155AssetDetailViewModel viewModel;

    private Token token;
    private Wallet wallet;

    private LinearLayout tokenInfoLayout;

    private TokenInfoView issuer;
    private TokenInfoView reserveValue;
    private TokenInfoView created;
    private TokenInfoView type;
    private TokenInfoView type2;
    private TokenInfoView transferFee;
    private TokenInfoView circulatingSupply;
    private TextView tokenDescription;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_erc1155_asset_detail);

        toolbar();

        getIntentData();

        setTitle(token.tokenInfo.name);

        initViews();

        initViewModel();

        setupFunctionBar();
    }

    private void initViews()
    {
        tokenInfoLayout = findViewById(R.id.layout_token_info);

        issuer = new TokenInfoView(this, "Issuer");
        reserveValue = new TokenInfoView(this, "Reserve Value");
        created = new TokenInfoView(this, "Created");
        type = new TokenInfoView(this, "Type");
        type2 = new TokenInfoView(this, "Type");
        transferFee = new TokenInfoView(this, "Transfer Fee");
        circulatingSupply = new TokenInfoView(this, "Circulating Supply");

        tokenDescription = findViewById(R.id.token_description);

        tokenInfoLayout.addView(new TokenInfoCategoryView(this, "Details"));
        tokenInfoLayout.addView(issuer);
        tokenInfoLayout.addView(reserveValue);
        tokenInfoLayout.addView(created);
        tokenInfoLayout.addView(type);
        tokenInfoLayout.addView(type2);
        tokenInfoLayout.addView(transferFee);
        tokenInfoLayout.addView(circulatingSupply);

        tokenInfoLayout.addView(new TokenInfoCategoryView(this, "Description"));
        tokenDescription.setText("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nam ut ante ut velit molestie rutrum. Praesent ut sapien vitae ex imperdiet efficitur id vel urna. Etiam ac gravida metus.");

    }

    private void getIntentData()
    {
        token = getIntent().getParcelableExtra(C.EXTRA_TOKEN_ID);
        wallet = getIntent().getParcelableExtra(C.Key.WALLET);
        // TODO: retrieve asset from intent
        // asset = getIntent().getParcelableExtra("asset");
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
            functionBar.setupFunctions(this, viewModel.getAssetDefinitionService(), token, null, null);
            functionBar.revealButtons();
            functionBar.setWalletType(wallet.type);
        }
    }
}
