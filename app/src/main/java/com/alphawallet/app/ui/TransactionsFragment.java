package com.alphawallet.app.ui;

import android.annotation.SuppressLint;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.TokenInterface;
import com.alphawallet.app.entity.TokensReceiver;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.ui.widget.adapter.TransactionsAdapter;

import dagger.android.support.AndroidSupportInjection;
import com.alphawallet.app.R;

import com.alphawallet.app.viewmodel.TransactionsViewModel;
import com.alphawallet.app.viewmodel.TransactionsViewModelFactory;
import com.alphawallet.app.widget.EmptyTransactionsView;
import com.alphawallet.app.widget.SystemView;

import javax.inject.Inject;

import static com.alphawallet.app.C.ErrorCode.EMPTY_COLLECTION;

public class TransactionsFragment extends Fragment implements View.OnClickListener, TokenInterface
{
    @Inject
    TransactionsViewModelFactory transactionsViewModelFactory;
    private TransactionsViewModel viewModel;

    private TokensReceiver tokenReceiver;

    private SystemView systemView;
    private TransactionsAdapter adapter;

    private boolean isVisible = false;
    private boolean firstView = true;

    RecyclerView list;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        AndroidSupportInjection.inject(this);
        View view = inflater.inflate(R.layout.fragment_transactions, container, false);

        viewModel = ViewModelProviders.of(this, transactionsViewModelFactory)
                .get(TransactionsViewModel.class);

        adapter = new TransactionsAdapter(this::onTransactionClick, viewModel.getTokensService(),
                                          viewModel.provideTransactionsInteract());
        SwipeRefreshLayout refreshLayout = view.findViewById(R.id.refresh_layout);
        systemView = view.findViewById(R.id.system_view);

        list = view.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        list.setAdapter(adapter);

        systemView.attachRecyclerView(list);
        systemView.attachSwipeRefreshLayout(refreshLayout);

        systemView.showProgress(false);

        viewModel.progress().observe(this, systemView::showProgress);
        viewModel.error().observe(this, this::onError);
        viewModel.defaultWallet().observe(this, this::onDefaultWallet);
        viewModel.transactions().observe(this, this::onTransactions);
        viewModel.showEmpty().observe(this, this::showEmptyTx);
        viewModel.clearAdapter().observe(this, this::clearAdapter);
        viewModel.refreshAdapter().observe(this, this::refreshAdapter);
        viewModel.newTransactions().observe(this, this::onNewTransactions);
        refreshLayout.setOnRefreshListener(() -> viewModel.prepare());

        adapter.clear();

        tokenReceiver = new TokensReceiver(getActivity(), this);

        return view;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.try_again: {
                viewModel.prepare();
            }
            break;
        }
    }

    private void onTransactionClick(View view, Transaction transaction) {
        viewModel.showDetails(view.getContext(), transaction);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onResume() {
        super.onResume();
        viewModel.prepare();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        isVisible = isVisibleToUser;
        if (isResumed()) { // fragment created
            if (isVisible) {
                if (firstView)
                {
                    adapter.notifyDataSetChanged(); //first time viewing, refresh each view element to ensure tokens are displayed
                    firstView = false;
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void onTransactions(Transaction[] transaction) {
        adapter.updateTransactions(transaction);
    }

    private void onNewTransactions(Transaction[] transactions)
    {
        adapter.addNewTransactions(transactions);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (getContext() != null) getContext().unregisterReceiver(tokenReceiver);
    }

    private void onDefaultWallet(Wallet wallet)
    {
        adapter.setDefaultWallet(wallet);
    }

    private void onError(ErrorEnvelope errorEnvelope) {
        if (errorEnvelope.code == EMPTY_COLLECTION || adapter.getItemCount() == 0) {
            showEmptyTx(true);
        }/* else {
            systemView.showError(getString(R.string.error_fail_load_transaction), this);
        }*/
    }

    private void showEmptyTx(boolean show) {
        if (show)
        {
            EmptyTransactionsView emptyView = new EmptyTransactionsView(getContext(), this);
            systemView.showEmpty(emptyView);
        }
        else
        {
            systemView.hide();
        }
    }

    @Override
    public void resetTokens()
    {
        //first abort the current operation
        adapter.clear();
        list.setAdapter(adapter);
        viewModel.clearProcesses();
    }

    @Override
    public void addedToken()
    {

    }

    @Override
    public void changedLocale()
    {
        //need to refresh the transaction view
        adapter.clear();
        list.setAdapter(adapter);
        viewModel.clearProcesses();
    }

    private void clearAdapter(Boolean aBoolean)
    {
        adapter.clear();
        list.setAdapter(adapter);
        viewModel.clearProcesses();
    }

    private void refreshAdapter(Boolean aBoolean)
    {
        adapter.notifyDataSetChanged();
    }

    void transactionsShowing()
    {
        if (adapter != null) adapter.notifyDataSetChanged();
    }
}
