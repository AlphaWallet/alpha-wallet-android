package com.alphawallet.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenCardMeta;
import com.alphawallet.app.ui.widget.TokensAdapterCallback;
import com.alphawallet.app.ui.widget.adapter.TokensAdapter;
import com.alphawallet.app.ui.widget.entity.SearchToolbarCallback;
import com.alphawallet.app.viewmodel.WalletViewModel;
import com.alphawallet.app.widget.SearchToolbar;

import java.math.BigInteger;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;


@AndroidEntryPoint
public class SearchActivity extends BaseActivity implements SearchToolbarCallback, TokensAdapterCallback
{
    private WalletViewModel viewModel;
    private TokensAdapter adapter;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_wallet_search);

        initViews();

        initViewModel();

        initList();
    }

    private void initViews()
    {
        recyclerView = findViewById(R.id.list);
        progressBar = findViewById(R.id.progress_bar);
        SearchToolbar searchBar = findViewById(R.id.search);
        searchBar.setSearchCallback(this);
        searchBar.getEditView().requestFocus();
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
    }

    @Override
    public void searchText(String search)
    {
        if (viewModel != null)
        {
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
}
