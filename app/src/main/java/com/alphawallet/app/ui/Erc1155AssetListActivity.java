package com.alphawallet.app.ui;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.ui.widget.OnAssetClickListener;
import com.alphawallet.app.ui.widget.adapter.Erc1155AssetListAdapter;
import com.alphawallet.app.ui.widget.divider.ListDivider;
import com.alphawallet.app.viewmodel.Erc1155AssetListViewModel;
import com.alphawallet.app.viewmodel.Erc1155AssetListViewModelFactory;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class Erc1155AssetListActivity extends BaseActivity implements StandardFunctionInterface, OnAssetClickListener {
    @Inject
    Erc1155AssetListViewModelFactory viewModelFactory;
    Erc1155AssetListViewModel viewModel;

    private Token token;
    private Wallet wallet;

    private RecyclerView recyclerView;
    private Erc1155AssetListAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_erc1155_asset_list);

        toolbar();

        getIntentData();

        setTitle(token.tokenInfo.name);

        initViews();

        initViewModel();

        viewModel.getAssets(token);
    }

    private void initViews()
    {
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new ListDivider(this));
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
                .get(Erc1155AssetListViewModel.class);
    }

    @Override
    public void onAssetClicked(NFTAsset asset)
    {
        viewModel.showAssetDetails(this, wallet, token, asset);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_select, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.action_select) {
            viewModel.openSelectionMode(this, token, wallet);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
