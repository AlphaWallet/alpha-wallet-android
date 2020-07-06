package com.alphawallet.app.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ActivityMeta;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionMeta;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletPage;
import com.alphawallet.app.interact.ActivityDataInteract;
import com.alphawallet.app.repository.entity.RealmTransaction;
import com.alphawallet.app.ui.widget.adapter.ActivityAdapter;
import com.alphawallet.app.ui.widget.adapter.RecycleViewDivider;
import com.alphawallet.app.viewmodel.ActivityViewModel;
import com.alphawallet.app.viewmodel.ActivityViewModelFactory;
import com.alphawallet.app.widget.EmptyTransactionsView;
import com.alphawallet.app.widget.SystemView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by JB on 26/06/2020.
 */
public class ActivityFragment extends BaseFragment implements View.OnClickListener, ActivityDataInteract
{
    @Inject
    ActivityViewModelFactory activityViewModelFactory;
    private ActivityViewModel viewModel;

    private SystemView systemView;
    private ActivityAdapter adapter;
    private RecyclerView listView;
    private Realm realm;
    private RealmResults<RealmTransaction> realmUpdates;
    private String realmId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        AndroidSupportInjection.inject(this);
        View view = inflater.inflate(R.layout.fragment_transactions, container, false);
        toolbar(view);
        setToolbarTitle(R.string.activity_label);
        initViewModel();
        initViews(view);
        return view;
    }

    private void initViewModel()
    {
        if (viewModel == null)
        {
            viewModel = ViewModelProviders.of(this, activityViewModelFactory)
                    .get(ActivityViewModel.class);
        }
    }

    private void onItemsLoaded(ActivityMeta[] activityItems)
    {
        adapter.updateActivityItems(activityItems);
        showEmptyTx();
        long lastUpdateTime = 0;

        for (ActivityMeta am : activityItems)
        {
            if (am instanceof TransactionMeta && am.timeStamp > lastUpdateTime) lastUpdateTime = am.timeStamp;
        }

        startTxListener(lastUpdateTime - 60*10); //adjust for timestamp delay
    }

    private void startTxListener(long lastUpdateTime)
    {
        String walletAddress = viewModel.defaultWallet().getValue() != null ? viewModel.defaultWallet().getValue().address : "";
        if (realmId == null || !realmId.equals(walletAddress))
        {
            if (realmUpdates != null) realmUpdates.removeAllChangeListeners();

            realmId = walletAddress;
            realm = viewModel.getRealmInstance(new Wallet(walletAddress));
            realmUpdates = realm.where(RealmTransaction.class).greaterThan("timeStamp", lastUpdateTime).findAllAsync();
            realmUpdates.addChangeListener(realmTransactions -> {
                List<TransactionMeta> metas = new ArrayList<>();
                //make list
                if (realmTransactions.size() == 0) return;
                for (RealmTransaction item : realmTransactions)
                {
                    if (viewModel.getTokensService().getNetworkFilters().contains(item.getChainId()))
                    {
                        boolean pendingTx = item.getBlockNumber().equals("0");
                        TransactionMeta newMeta = new TransactionMeta(item.getHash(), item.getTimeStamp(), item.getTo(), item.getChainId(), pendingTx);
                        metas.add(newMeta);
                    }
                }

                if (metas.size() > 0)
                {
                    adapter.updateActivityItems(metas.toArray(new TransactionMeta[0]));
                    systemView.hide();
                }

                //Check for new unknown tokens
                viewModel.checkTokens(realmTransactions);
            });
        }
    }

    private void initViews(View view)
    {
        adapter = new ActivityAdapter(this::onActivityClick, viewModel.getTokensService(),
                viewModel.provideTransactionsInteract(), this);
        SwipeRefreshLayout refreshLayout = view.findViewById(R.id.refresh_layout);
        systemView = view.findViewById(R.id.system_view);
        listView = view.findViewById(R.id.list);
        listView.setLayoutManager(new LinearLayoutManager(getContext()));
        listView.setAdapter(adapter);
        listView.addItemDecoration(new RecycleViewDivider(getContext()));

        systemView.attachRecyclerView(listView);
        systemView.attachSwipeRefreshLayout(refreshLayout);

        systemView.showProgress(false);

        viewModel.defaultWallet().observe(this, this::onDefaultWallet);
        viewModel.activityItems().observe(this, this::onItemsLoaded);
        refreshLayout.setOnRefreshListener(this::refreshTransactionList);
    }

    private void onDefaultWallet(Wallet wallet)
    {
        adapter.setDefaultWallet(wallet);
    }

    private void onActivityClick(View view, Transaction transaction)
    {
        viewModel.showDetails(view.getContext(), transaction);
    }

    private void showEmptyTx()
    {
        if (adapter.getItemCount() == 0)
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
     * 1. ListView + generic update
     * 2. Move transaction update to tokensService
     * 2. Fetch transactions from chain (?)
     * 3. Fetch events relating to the user from contracts
     * 4.
     */

    @Override
    public void onClick(View v)
    {

    }

    private void refreshTransactionList()
    {
        //clear tx list and reload
        adapter.clear();
        viewModel.prepare();
    }

    public void resetTokens()
    {
        //wallet changed, reset
        adapter.clear();
        viewModel.prepare();

    }

    public void addedToken(List<ContractLocator> tokenContracts)
    {
        adapter.updateItems(tokenContracts);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (realmUpdates != null) realmUpdates.removeAllChangeListeners();
        if (realm != null && !realm.isClosed()) realm.close();
        if (viewModel != null) viewModel.onDestroy();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (viewModel == null)
        {
            ((HomeActivity)getActivity()).resetFragment(WalletPage.ACTIVITY);
        }
        else
        {
            viewModel.prepare();
        }
    }

    @Override
    public void fetchMoreData(long latestDate)
    {
        viewModel.fetchMoreTransactions(latestDate);
    }
}
