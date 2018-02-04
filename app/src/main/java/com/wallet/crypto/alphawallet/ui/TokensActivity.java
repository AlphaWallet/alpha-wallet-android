package com.wallet.crypto.trustapp.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.entity.ErrorEnvelope;
import com.wallet.crypto.trustapp.entity.Token;
import com.wallet.crypto.trustapp.ui.widget.adapter.TokensAdapter;
import com.wallet.crypto.trustapp.viewmodel.TokensViewModel;
import com.wallet.crypto.trustapp.viewmodel.TokensViewModelFactory;
import com.wallet.crypto.trustapp.widget.SystemView;

import java.math.BigDecimal;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.wallet.crypto.trustapp.C.ErrorCode.EMPTY_COLLECTION;
import static com.wallet.crypto.trustapp.C.Key.WALLET;

public class TokensActivity extends BaseActivity implements View.OnClickListener {
    @Inject
    TokensViewModelFactory transactionsViewModelFactory;
    private TokensViewModel viewModel;

    private SystemView systemView;
    private TokensAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tokens);

        toolbar();

        adapter = new TokensAdapter(this::onTokenClick);
        SwipeRefreshLayout refreshLayout = findViewById(R.id.refresh_layout);
        systemView = findViewById(R.id.system_view);

        RecyclerView list = findViewById(R.id.list);

        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        systemView.attachRecyclerView(list);
        systemView.attachSwipeRefreshLayout(refreshLayout);

        viewModel = ViewModelProviders.of(this, transactionsViewModelFactory)
                .get(TokensViewModel.class);
        viewModel.progress().observe(this, systemView::showProgress);
        viewModel.error().observe(this, this::onError);
        viewModel.tokens().observe(this, this::onTokens);
        viewModel.total().observe(this, this::onTotal);
        viewModel.wallet().setValue(getIntent().getParcelableExtra(WALLET));

        refreshLayout.setOnRefreshListener(viewModel::fetchTokens);
    }

    private void onTotal(BigDecimal totalInCurrency) {
        adapter.setTotal(totalInCurrency);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add, menu);
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add: {
                viewModel.showAddToken(this);
            } break;
            case R.id.action_edit: {
                viewModel.showEditTokens(this);
            } break;
            case android.R.id.home: {
                adapter.clear();
                viewModel.showTransactions(this);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        viewModel.showTransactions(this);
    }

    private void onTokenClick(View view, Token token) {
        Context context = view.getContext();
        token.tokenInfo.clickReact(viewModel, context, token.balance.intValue(), token);
    }

    @Override
    protected void onResume() {
        super.onResume();

        viewModel.fetchTokens();
    }

    private void onTokens(Token[] tokens) {
        adapter.setTokens(tokens);
    }

    private void onError(ErrorEnvelope errorEnvelope) {
        if (errorEnvelope.code == EMPTY_COLLECTION) {
            systemView.showEmpty(getString(R.string.no_tokens));
        } else {
            systemView.showError(getString(R.string.error_fail_load_tokens), this);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.try_again: {
                viewModel.fetchTokens();
            } break;
        }
    }
}
