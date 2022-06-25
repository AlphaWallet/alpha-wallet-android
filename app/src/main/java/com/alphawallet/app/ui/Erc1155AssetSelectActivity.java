package com.alphawallet.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.ui.widget.OnAssetSelectListener;
import com.alphawallet.app.ui.widget.adapter.Erc1155AssetSelectAdapter;
import com.alphawallet.app.ui.widget.entity.QuantitySelectorDialogInterface;
import com.alphawallet.app.viewmodel.Erc1155AssetSelectViewModel;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.app.widget.QuantitySelectorDialog;
import com.alphawallet.ethereum.EthereumNetworkBase;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class Erc1155AssetSelectActivity extends BaseActivity
        implements StandardFunctionInterface, OnAssetSelectListener, QuantitySelectorDialogInterface
{
    private Erc1155AssetSelectViewModel viewModel;
    private QuantitySelectorDialog dialog;
    private RecyclerView recyclerView;
    private Erc1155AssetSelectAdapter adapter;
    private Token token;
    private Wallet wallet;
    private List<BigInteger> tokenIds;

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
        setContentView(R.layout.activity_erc1155_asset_select);
        toolbar();
        setTitle(getString(R.string.title_x_selected, "0"));
        initViewModel();
        getIntentData();
        initViews();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        viewModel.getAssets(token, tokenIds);
    }

    private void initViews()
    {
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        dialog = new QuantitySelectorDialog(this, this);
    }

    private void getIntentData()
    {
        String address = getIntent().getStringExtra(C.EXTRA_ADDRESS);
        long chainId = getIntent().getLongExtra(C.EXTRA_CHAIN_ID, EthereumNetworkBase.MAINNET_ID);
        token = viewModel.getTokensService().getToken(chainId, address);
        wallet = getIntent().getParcelableExtra(C.Key.WALLET);
        String tokenIdStr = getIntent().getStringExtra(C.EXTRA_TOKEN_ID);
        tokenIds = !TextUtils.isEmpty(tokenIdStr) ? token.stringHexToBigIntegerList(tokenIdStr)
                : new ArrayList<>();
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this)
                .get(Erc1155AssetSelectViewModel.class);
        viewModel.assets().observe(this, this::onAssets);
    }

    private void setupFunctionBar()
    {
        if (BuildConfig.DEBUG || wallet.type != WalletType.WATCH)
        {
            FunctionButtonBar functionBar = findViewById(R.id.layoutButtons);
            functionBar.setupFunctions(this, viewModel.getAssetDefinitionService(), token, adapter, null);
            functionBar.revealButtons();
            functionBar.setWalletType(wallet.type);
        }
    }

    private void onAssets(Map<BigInteger, NFTAsset> assets)
    {
        adapter = new Erc1155AssetSelectAdapter(this, assets, this);
        recyclerView.setAdapter(adapter);
        setupFunctionBar();
    }

    private void updateTitle()
    {
        List<NFTAsset> selectedAssets = adapter.getSelectedAssets();
        setTitle(getString(R.string.title_x_selected, String.valueOf(selectedAssets.size())));
    }

    @Override
    public void onAssetSelected(BigInteger tokenId, NFTAsset asset, int position)
    {
        updateTitle();

        if (asset.isAssetMultiple())
        {
            int balance = asset.getBalance().intValue();
            dialog.init(balance, position);
            dialog.show();
        }
        else
        {
            adapter.setSelectedAmount(position, 1);
        }
    }

    @Override
    public void onAssetUnselected()
    {
        updateTitle();
    }

    @Override
    public void showTransferToken(List<BigInteger> selection)
    {
        Intent launchTransfer = viewModel.completeTransferIntent(this, token, adapter.getSelectedTokenIds(), adapter.getSelectedAssets(), wallet);
        handleTransactionSuccess.launch(launchTransfer);
    }

    @Override
    public void onConfirm(int position, int quantity)
    {
        adapter.setSelectedAmount(position, quantity);
    }

    @Override
    public void onCancel(int position)
    {
        adapter.setSelectedAmount(position, 0);
        updateTitle();
    }
}
