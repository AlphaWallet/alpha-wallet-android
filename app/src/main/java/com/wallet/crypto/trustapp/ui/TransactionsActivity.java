package com.wallet.crypto.trustapp.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.wallet.crypto.trustapp.C;
import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.entity.ErrorEnvelope;
import com.wallet.crypto.trustapp.entity.NetworkInfo;
import com.wallet.crypto.trustapp.entity.Transaction;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.ui.widget.adapter.TransactionsAdapter;
import com.wallet.crypto.trustapp.viewmodel.BaseNavigationActivity;
import com.wallet.crypto.trustapp.viewmodel.TransactionsViewModel;
import com.wallet.crypto.trustapp.viewmodel.TransactionsViewModelFactory;
import com.wallet.crypto.trustapp.widget.SystemView;

import java.util.Map;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.wallet.crypto.trustapp.C.ETH_SYMBOL;

public class TransactionsActivity extends BaseNavigationActivity implements View.OnClickListener {

    @Inject
    TransactionsViewModelFactory transactionsViewModelFactory;
    private TransactionsViewModel viewModel;
    private SystemView systemView;
    private TransactionsAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_transactions);

        toolbar();
        initBottomNavigation();
        dissableDisplayHomeAsUp();

        adapter = new TransactionsAdapter(this::onTransactionClick);
        SwipeRefreshLayout refreshLayout = findViewById(R.id.refresh_layout);
        systemView = findViewById(R.id.system_view);

        RecyclerView list = findViewById(R.id.list);

        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        systemView.attachRecyclerView(list);
        systemView.attachSwipeRefreshLayout(refreshLayout);

        viewModel = ViewModelProviders.of(this, transactionsViewModelFactory)
                .get(TransactionsViewModel.class);
        viewModel.progress().observe(this, systemView::showProgress);
        viewModel.error().observe(this, this::onError);
        viewModel.defaultNetwork().observe(this, this::onDefaultNetwork);
        viewModel.defaultWalletBalance().observe(this, this::onBalanceChanged);
        viewModel.defaultWallet().observe(this, this::onDefaultWallet);
        viewModel.transactions().observe(this, this::onTransactions);

        refreshLayout.setOnRefreshListener(viewModel::fetchTransactions);
    }

    private void onTransactionClick(View view, Transaction transaction) {
        viewModel.showDetails(view.getContext(), transaction);
    }

    @Override
    protected void onResume() {
        super.onResume();

        adapter.clear();
        viewModel.prepare();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.transaction_list_menu, menu);

        NetworkInfo networkInfo = viewModel.defaultNetwork().getValue();
        if (networkInfo != null && networkInfo.symbol.equals(ETH_SYMBOL)) {
            getMenuInflater().inflate(R.menu.menu_deposit, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_wallets: {
                viewModel.openWallets(this);
            } break;
            case R.id.action_settings: {
                viewModel.openSettings(this);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void onBalanceChanged(Map<String, String> ballances) {
        ActionBar actionBar = getSupportActionBar();
        NetworkInfo networkInfo = viewModel.defaultNetwork().getValue();
        Wallet wallet = viewModel.defaultWallet().getValue();
        if (actionBar == null || networkInfo == null || wallet == null) {
            return;
        }
        if (TextUtils.isEmpty(ballances.get(C.USD_SYMBOL))) {
            actionBar.setTitle(ballances.get(networkInfo.symbol) + " " + networkInfo.symbol);
            actionBar.setSubtitle(wallet.address);
        } else {
            actionBar.setTitle("$" + ballances.get(C.USD_SYMBOL));
            actionBar.setSubtitle(ballances.get(C.USD_SYMBOL) + " " + networkInfo.symbol);
        }
    }

    private void onTransactions(Transaction[] transaction) {
        adapter.addTransactions(transaction);
    }

    private void onDefaultWallet(Wallet wallet) {
        adapter.setDefaultWallet(wallet);
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        adapter.setDefaultNetwork(networkInfo);
        setBottomMenu(networkInfo.isMainNetwork
                ? R.menu.menu_main_network : R.menu.menu_secondary_network);
    }

    private void onError(ErrorEnvelope errorEnvelope) {
        systemView.showError(getString(R.string.error_fail_load_transaction), this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.try_again: {
                viewModel.fetchTransactions();
            } break;
        }
    }
}
