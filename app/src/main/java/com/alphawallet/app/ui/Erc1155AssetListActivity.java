package com.alphawallet.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.alphawallet.app.viewmodel.Erc1155AssetListViewModel;
import com.alphawallet.ethereum.EthereumNetworkBase;

import java.math.BigInteger;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class Erc1155AssetListActivity extends BaseActivity implements StandardFunctionInterface, OnAssetClickListener
{
    private Erc1155AssetListViewModel viewModel;
    private Token token;
    private Wallet wallet;
    private NFTAsset asset;
    private RecyclerView recyclerView;
    private Erc1155AssetListAdapter adapter;

    private ActivityResultLauncher<Intent> handleTransactionSuccess = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
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
        setContentView(R.layout.activity_erc1155_asset_list);

        toolbar();

        initViewModel();

        getIntentData();

        setTitle(token.tokenInfo.name);

        initViews();

        adapter = new Erc1155AssetListAdapter(this, token.getTokenAssets(), asset, this);
        recyclerView.setAdapter(adapter);
    }

    private void initViews()
    {
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void getIntentData()
    {
        long chainId = getIntent().getLongExtra(C.EXTRA_CHAIN_ID, EthereumNetworkBase.MAINNET_ID);
        token = viewModel.getTokensService().getToken(chainId, getIntent().getStringExtra(C.EXTRA_ADDRESS));
        wallet = getIntent().getParcelableExtra(C.Key.WALLET);
        asset = getIntent().getParcelableExtra(C.EXTRA_NFTASSET_LIST);
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this)
                .get(Erc1155AssetListViewModel.class);
    }

    @Override
    public void onAssetClicked(Pair<BigInteger, NFTAsset> pair)
    {
        handleTransactionSuccess.launch(viewModel.showAssetDetailsIntent(this, wallet, token, pair.first));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_nft_display, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.action_send_multiple_tokens)
        {
            handleTransactionSuccess.launch(viewModel.openSelectionModeIntent(this, token, wallet, asset));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
