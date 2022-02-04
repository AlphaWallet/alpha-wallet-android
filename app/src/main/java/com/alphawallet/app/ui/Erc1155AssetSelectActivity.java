package com.alphawallet.app.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

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
import com.alphawallet.app.entity.tokens.ERC1155Token;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.ui.widget.OnAssetSelectListener;
import com.alphawallet.app.ui.widget.adapter.Erc1155AssetSelectAdapter;
import com.alphawallet.app.ui.widget.divider.ListDivider;
import com.alphawallet.app.ui.widget.entity.AmountReadyCallback;
import com.alphawallet.app.ui.widget.entity.NumericInputBottomSheet;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.Erc1155AssetSelectViewModel;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.ethereum.EthereumNetworkBase;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class Erc1155AssetSelectActivity extends BaseActivity implements StandardFunctionInterface, OnAssetSelectListener, AmountReadyCallback
{

    Erc1155AssetSelectViewModel viewModel;
    List<NFTAsset> selectedAssets = new ArrayList<>();
    private Token token;
    private Wallet wallet;
    private List<BigInteger> tokenIds;
    private RecyclerView recyclerView;
    private Erc1155AssetSelectAdapter adapter;
    private NumericInputBottomSheet numericInput;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_erc1155_asset_select);
        toolbar();
        setTitle(getString(R.string.title_x_selected, "0"));

        initViewModel();
        getIntentData();

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new ListDivider(this));

        numericInput = findViewById(R.id.numeric_input);

        viewModel.getAssets(token, tokenIds);
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

    @Override
    public void onAssetSelected(BigInteger tokenId, NFTAsset asset, int position)
    {
        selectedAssets = adapter.getSelectedAssets();
        setTitle(getString(R.string.title_x_selected, String.valueOf(selectedAssets.size())));

        if (asset.isAssetMultiple())
        {
            int selectedValue = asset.getSelectedBalance().intValue() > 0 ? asset.getSelectedBalance().intValue() : 1;
            numericInput.setVisibility(View.VISIBLE);
            numericInput.initAmount(asset.getBalance().intValue(), selectedValue, this, position);
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(numericInput.getInputField(), InputMethodManager.SHOW_IMPLICIT);
            FunctionButtonBar functionBar = findViewById(R.id.layoutButtons);
            functionBar.setVisibility(View.GONE);
        }
        else
        {
            numericInput.completeLastSelection(position);
            adapter.setSelectedAmount(position, 1);
            removeInput();
        }
    }

    @Override
    public void onAssetUnselected()
    {
        selectedAssets = adapter.getSelectedAssets();
        setTitle(getString(R.string.title_x_selected, String.valueOf(selectedAssets.size())));
        if (numericInput.getVisibility() == View.VISIBLE)
        {
            removeInput();
        }
    }

    @Override
    public void amountReady(BigDecimal value, BigDecimal position)
    {
        //got new amount from widget
        adapter.setSelectedAmount(position.intValue(), value.intValue());
        FunctionButtonBar functionBar = findViewById(R.id.layoutButtons);
        functionBar.setVisibility(View.VISIBLE);
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
        Intent launchTransfer = viewModel.completeTransferIntent(this, token, adapter.getSelectedTokenIds(), adapter.getSelectedAssets(), wallet);
        handleTransactionSuccess.launch(launchTransfer);
    }

    private void removeInput()
    {
        numericInput.setVisibility(View.GONE);
        FunctionButtonBar functionBar = findViewById(R.id.layoutButtons);
        functionBar.setVisibility(View.VISIBLE);
    }
}
