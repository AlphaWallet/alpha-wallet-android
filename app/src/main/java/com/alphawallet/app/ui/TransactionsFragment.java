package com.alphawallet.app.ui;

import android.annotation.SuppressLint;
import androidx.lifecycle.ViewModelProviders;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.Event;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.TokenInterface;
import com.alphawallet.app.entity.tokens.TokensReceiver;
import com.alphawallet.app.ui.widget.adapter.RecycleViewDivider;
import com.alphawallet.app.ui.widget.adapter.TransactionsAdapter;
import com.alphawallet.app.viewmodel.TransactionsViewModel;
import com.alphawallet.app.viewmodel.TransactionsViewModelFactory;
import com.alphawallet.app.widget.AWalletBottomNavigationView;
import com.alphawallet.app.widget.EmptyTransactionsView;
import com.alphawallet.app.widget.SystemView;

import java.util.List;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;

public class TransactionsFragment extends BaseFragment implements View.OnClickListener, TokenInterface
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
        toolbar(view);
        setToolbarTitle(R.string.toolbar_header_transactions);
        initViewModel(view);

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

    private void initViewModel(View view)
    {
        if (viewModel != null) return;

        viewModel = ViewModelProviders.of(this, transactionsViewModelFactory)
                .get(TransactionsViewModel.class);

        adapter = new TransactionsAdapter(this::onTransactionClick, viewModel.getTokensService(),
                                          viewModel.provideTransactionsInteract());
        SwipeRefreshLayout refreshLayout = view.findViewById(R.id.refresh_layout);
        systemView = view.findViewById(R.id.system_view);

        list = view.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        list.setAdapter(adapter);
        list.addItemDecoration(new RecycleViewDivider(getContext()));

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
        viewModel.event().observe(this, this::onNewEvents);
        refreshLayout.setOnRefreshListener(() -> viewModel.prepare());

        adapter.clear();

        tokenReceiver = new TokensReceiver(getActivity(), this);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onResume() {
        super.onResume();
        if (viewModel == null)
        {
            ((HomeActivity)getActivity()).resetFragment(AWalletBottomNavigationView.TRANSACTIONS);
        }
        else
        {
            viewModel.prepare();
        }
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

    private void onTransactions(Transaction[] transactions) {
        adapter.updateTransactions(transactions);
        if (transactions.length > 0) showEmptyTx(false);
    }

    private void onNewTransactions(Transaction[] transactions)
    {
        adapter.addNewTransactions(transactions);
        if (transactions.length > 0) showEmptyTx(false);
    }

    private void onNewEvents(Event[] events)
    {
        adapter.addEvents(events);
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

    }

    private void showEmptyTx(boolean show)
    {
        if (show && adapter.getItemCount() == 0)
        {
            EmptyTransactionsView emptyView = new EmptyTransactionsView(getContext(), this);
            systemView.showEmpty(emptyView);
        }
        else
        {
            systemView.hide();
        }
    }

    /**
     * Called only after user changes the wallet
     */
    @Override
    public void resetTokens()
    {
        //first abort the current operation
        adapter.clear();
        list.setAdapter(adapter);
        viewModel.clearProcesses();
        viewModel.restartEventListener();
    }

    @Override
    public void addedToken(List<ContractLocator> tokenContracts)
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
