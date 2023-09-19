package com.alphawallet.app.ui;

import static com.alphawallet.app.C.RESET_WALLET;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.ui.widget.TokensAdapterCallback;
import com.alphawallet.app.ui.widget.adapter.TokensAdapter;
import com.alphawallet.app.ui.widget.entity.SearchToolbarCallback;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.WalletViewModel;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.SearchToolbar;

import org.web3j.crypto.WalletUtils;

import java.math.BigInteger;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;


@AndroidEntryPoint
public class SearchActivity extends BaseActivity implements SearchToolbarCallback, TokensAdapterCallback
{
    private ActivityResultLauncher<Intent> addTokenLauncher;
    private WalletViewModel viewModel;
    private TokensAdapter adapter;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private String searchText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_wallet_search);

        initViews();

        initViewModel();

        initList();

        initIntentLaunchers();
    }

    private void initViews()
    {
        recyclerView = findViewById(R.id.list);
        progressBar = findViewById(R.id.progress_bar);
        SearchToolbar searchBar = findViewById(R.id.search);
        searchBar.setSearchCallback(this);
        searchBar.getEditView().requestFocus();
        searchText = "";
    }

    private void initViewModel()
    {
        viewModel = new ViewModelProvider(this)
                .get(WalletViewModel.class);
        viewModel.tokens().observe(this, this::onTokens);
        viewModel.prepare();
    }

    private void initList()
    {
        adapter = new TokensAdapter(
                this,
                viewModel.getAssetDefinitionService(),
                viewModel.getTokensService(),
                null);
        adapter.setHasStableIds(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void onTokens(TokenCardMeta[] tokens)
    {
        adapter.setTokens(tokens);
        viewModel.calculateFiatValues();
        progressBar.setVisibility(View.GONE);

        //If no results, try searching
        if (tokens.length == 0)
        {
            searchForToken(searchText);
        }
    }

    public void searchForToken(String searchString)
    {
        if (!TextUtils.isEmpty(searchString) && WalletUtils.isValidAddress(searchString))
        {
            Intent intent = new Intent(this, AddTokenActivity.class);
            intent.putExtra(C.EXTRA_ADDRESS, searchString);
            addTokenLauncher.launch(intent);
        }
    }

    @Override
    public void searchText(String search)
    {
        if (viewModel != null)
        {
            searchText = search;
            viewModel.searchTokens(search);
            adapter.clear();
        }
    }

    @Override
    public void backPressed()
    {
        finish();
    }

    @Override
    public void onTokenClick(View view, Token token, List<BigInteger> tokenIds, boolean selected)
    {
        Token clickOrigin = viewModel.getTokenFromService(token);
        if (clickOrigin == null) clickOrigin = token;
        viewModel.showTokenDetail(this, clickOrigin);
    }

    @Override
    public void onLongTokenClick(View view, Token token, List<BigInteger> tokenIds)
    {

    }

    private void initIntentLaunchers()
    {
        addTokenLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    //finish and return
                    Intent intent = new Intent();
                    intent.putExtra(RESET_WALLET, true);
                    setResult(RESULT_OK, intent);
                    finish();
                });
    }
}
