package com.wallet.crypto.trustapp.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.entity.ErrorEnvelope;
import com.wallet.crypto.trustapp.entity.Token;
import com.wallet.crypto.trustapp.ui.widget.adapter.TokensAdapter;
import com.wallet.crypto.trustapp.viewmodel.TokensViewModel;
import com.wallet.crypto.trustapp.viewmodel.TokensViewModelFactory;
import com.wallet.crypto.trustapp.widget.SystemView;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

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
        viewModel.wallet().setValue(getIntent().getParcelableExtra(WALLET));

        refreshLayout.setOnRefreshListener(viewModel::fetchTokens);
    }

    private void onTokenClick(View view, Token token) {
//        Context context = v.getContext();
//        Intent intent = new Intent(context, SendActivity.class);
//        intent.putExtra(SendActivity.EXTRA_SENDING_TOKENS, true);
//        intent.putExtra(SendActivity.EXTRA_CONTRACT_ADDRESS, info.getAddress());
//        intent.putExtra(SendActivity.EXTRA_SYMBOL, info.getSymbol());
//        intent.putExtra(SendActivity.EXTRA_DECIMALS, info.getDecimals());
//
//        context.startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        viewModel.prepare();
    }

    private void onTokens(Token[] tokens) {
        adapter.setTokens(tokens);
    }

    private void onError(ErrorEnvelope errorEnvelope) {
        systemView.showError(getString(R.string.error_fail_load_transaction), this);
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
