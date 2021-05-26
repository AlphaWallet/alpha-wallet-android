package com.alphawallet.app.ui;

import android.os.Bundle;
import android.view.Menu;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.tokens.ERC1155Asset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.ui.widget.OnAssetSelectListener;
import com.alphawallet.app.ui.widget.adapter.Erc1155AssetSelectAdapter;
import com.alphawallet.app.ui.widget.divider.ListDivider;
import com.alphawallet.app.viewmodel.Erc1155AssetSelectViewModel;
import com.alphawallet.app.viewmodel.Erc1155AssetSelectViewModelFactory;
import com.alphawallet.app.widget.FunctionButtonBar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class Erc1155AssetSelectActivity extends BaseActivity implements StandardFunctionInterface, OnAssetSelectListener {
    @Inject
    Erc1155AssetSelectViewModelFactory viewModelFactory;
    Erc1155AssetSelectViewModel viewModel;
    List<ERC1155Asset> selectedAssets = new ArrayList<>();
    private Menu menu;
    private Token token;
    private Wallet wallet;
    private RecyclerView recyclerView;
    private Erc1155AssetSelectAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_erc1155_asset_select);
        toolbar();
        setTitle(getString(R.string.title_x_selected, "0"));

        getIntentData();

        initViewModel();

        setupFunctionBar();

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new ListDivider(this));

        viewModel.getAssets(token);
    }

    private void getIntentData()
    {
        token = getIntent().getParcelableExtra(C.EXTRA_TOKEN_ID);
        wallet = getIntent().getParcelableExtra(C.Key.WALLET);
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(Erc1155AssetSelectViewModel.class);
        viewModel.assets().observe(this, this::onAssets);
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

    private void onAssets(Map<Long, ERC1155Asset> assets)
    {
        adapter = new Erc1155AssetSelectAdapter(this, assets, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onAssetSelected()
    {
        selectedAssets = adapter.getSelectedAssets();
        setTitle(getString(R.string.title_x_selected, String.valueOf(selectedAssets.size())));
    }

    @Override
    public void onAssetUnselected()
    {
        selectedAssets = adapter.getSelectedAssets();
        setTitle(getString(R.string.title_x_selected, String.valueOf(selectedAssets.size())));
    }
}
