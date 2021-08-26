package com.alphawallet.app.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

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
import com.alphawallet.app.ui.widget.divider.ListDivider;
import com.alphawallet.app.ui.widget.entity.AmountReadyCallback;
import com.alphawallet.app.ui.widget.entity.NumericInputBottomSheet;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.Erc1155AssetSelectViewModel;
import com.alphawallet.app.viewmodel.Erc1155AssetSelectViewModelFactory;
import com.alphawallet.app.widget.FunctionButtonBar;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class Erc1155AssetSelectActivity extends BaseActivity implements StandardFunctionInterface, OnAssetSelectListener, AmountReadyCallback
{
    @Inject
    Erc1155AssetSelectViewModelFactory viewModelFactory;
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
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_erc1155_asset_select);
        toolbar();
        setTitle(getString(R.string.title_x_selected, "0"));

        getIntentData();

        initViewModel();

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new ListDivider(this));

        numericInput = findViewById(R.id.numeric_input);

        viewModel.getAssets(token, tokenIds);
    }

    private void getIntentData()
    {
        token = getIntent().getParcelableExtra(C.EXTRA_TOKEN);
        wallet = getIntent().getParcelableExtra(C.Key.WALLET);
        String tokenIdStr = getIntent().getStringExtra(C.EXTRA_TOKEN_ID);
        tokenIds = !TextUtils.isEmpty(tokenIdStr) ? token.stringHexToBigIntegerList(tokenIdStr)
                : new ArrayList<>();
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
    public void onAssetSelected(NFTAsset asset, int position)
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

    @Override
    public void showTransferToken(List<BigInteger> selection)
    {
        viewModel.completeTransfer(this, token, adapter.getSelectedTokenIds(), adapter.getSelectedAssets(), wallet);
    }

    private void removeInput()
    {
        numericInput.setVisibility(View.GONE);
        FunctionButtonBar functionBar = findViewById(R.id.layoutButtons);
        functionBar.setVisibility(View.VISIBLE);
    }
}
