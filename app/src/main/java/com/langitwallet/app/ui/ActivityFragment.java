package com.langitwallet.app.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.langitwallet.app.R;
import com.langitwallet.app.analytics.Analytics;
import com.langitwallet.app.entity.ActivityMeta;
import com.langitwallet.app.entity.ContractLocator;
import com.langitwallet.app.entity.TransactionMeta;
import com.langitwallet.app.entity.Wallet;
import com.langitwallet.app.interact.ActivityDataInteract;
import com.langitwallet.app.repository.entity.RealmTransaction;
import com.langitwallet.app.repository.entity.RealmTransfer;
import com.langitwallet.app.ui.widget.adapter.ActivityAdapter;
import com.langitwallet.app.ui.widget.entity.TokenTransferData;
import com.langitwallet.app.util.LocaleUtils;
import com.langitwallet.app.viewmodel.ActivityViewModel;
import com.langitwallet.app.widget.EmptyTransactionsView;
import com.langitwallet.app.widget.SystemView;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by JB on 26/06/2020.
 */
@AndroidEntryPoint
public class ActivityFragment extends BaseFragment implements View.OnClickListener, ActivityDataInteract
{
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ActivityViewModel viewModel;
    private SystemView systemView;
    private ActivityAdapter adapter;
    private RecyclerView listView;
    private RealmResults<RealmTransaction> realmUpdates;
    private boolean checkTimer;
    private Realm realm;
    private long lastUpdateTime = 0;
    private boolean isVisible = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        LocaleUtils.setActiveLocale(requireContext());
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
            viewModel = new ViewModelProvider(this)
                    .get(ActivityViewModel.class);
            viewModel.defaultWallet().observe(getViewLifecycleOwner(), this::onDefaultWallet);
            viewModel.activityItems().observe(getViewLifecycleOwner(), this::onItemsLoaded);
        }
    }

    private void onItemsLoaded(ActivityMeta[] activityItems)
    {
        try (Realm realm = viewModel.getRealmInstance())
        {
            adapter.updateActivityItems(buildTransactionList(realm, activityItems).toArray(new ActivityMeta[0]));
            showEmptyTx();

            for (ActivityMeta am : activityItems)
            {
                if (am instanceof TransactionMeta && am.getTimeStampSeconds() > lastUpdateTime)
                    lastUpdateTime = am.getTimeStampSeconds() - 60;
            }
        }

        if (isVisible) startTxListener();
    }

    private void startTxListener()
    {
        if (viewModel.defaultWallet().getValue() == null) return;
        if (realm == null || realm.isClosed()) realm = viewModel.getRealmInstance();
        if (realmUpdates != null) realmUpdates.removeAllChangeListeners();
        if (viewModel == null || viewModel.defaultWallet().getValue() == null || TextUtils.isEmpty(viewModel.defaultWallet().getValue().address))
            return;

        realmUpdates = realm.where(RealmTransaction.class).greaterThan("timeStamp", lastUpdateTime).findAllAsync();
        realmUpdates.addChangeListener(realmTransactions -> {
            List<TransactionMeta> metas = new ArrayList<>();
            //make list
            if (realmTransactions.size() == 0) return;
            for (RealmTransaction item : realmTransactions)
            {
                if (viewModel.getTokensService().getNetworkFilters().contains(item.getChainId()))
                {
                    TransactionMeta newMeta = new TransactionMeta(item.getHash(), item.getTimeStamp(), item.getTo(), item.getChainId(), item.getBlockNumber());
                    metas.add(newMeta);
                    lastUpdateTime = newMeta.getTimeStampSeconds() + 1;
                }
            }

            if (metas.size() > 0)
            {
                TransactionMeta[] metaArray = metas.toArray(new TransactionMeta[0]);
                adapter.updateActivityItems(buildTransactionList(realm, metaArray).toArray(new ActivityMeta[0]));
                systemView.hide();
            }
        });
    }

    private List<ActivityMeta> buildTransactionList(Realm realm, ActivityMeta[] activityItems)
    {
        //selectively filter the items with the following rules:
        // - allow through all normal transactions with no token transfer consequences
        // - for any transaction with token transfers; if there's only one token transfer, only show the transfer
        // - for any transaction with more than one token transfer, show the transaction and show the child transfer consequences
        List<ActivityMeta> filteredList = new ArrayList<>();

        for (ActivityMeta am : activityItems)
        {
            if (am instanceof TransactionMeta)
            {
                List<TokenTransferData> tokenTransfers = getTokenTransfersForHash(realm, (TransactionMeta) am);
                if (tokenTransfers.size() != 1)
                {
                    filteredList.add(am);
                } //only 1 token transfer ? No need to show the underlying transaction
                filteredList.addAll(tokenTransfers);
            }
        }

        return filteredList;
    }

    private List<TokenTransferData> getTokenTransfersForHash(Realm realm, TransactionMeta tm)
    {
        List<TokenTransferData> transferData = new ArrayList<>();
        //summon realm items
        //get matching entries for this transaction
        RealmResults<RealmTransfer> transfers = realm.where(RealmTransfer.class)
                .equalTo("hash", RealmTransfer.databaseKey(tm.chainId, tm.hash))
                .findAll();

        if (transfers != null && transfers.size() > 0)
        {
            //list of transfers, descending in time to give ordered list
            long nextTransferTime = transfers.size() == 1 ? tm.getTimeStamp() : tm.getTimeStamp() - 1; // if there's only 1 transfer, keep the transaction timestamp
            for (RealmTransfer rt : transfers)
            {
                TokenTransferData ttd = new TokenTransferData(rt.getHash(), tm.chainId,
                        rt.getTokenAddress(), rt.getEventName(), rt.getTransferDetail(), nextTransferTime);
                transferData.add(ttd);
                nextTransferTime--;
            }
        }

        return transferData;
    }

    private void initViews(View view)
    {
        adapter = new ActivityAdapter(viewModel.getTokensService(), viewModel.provideTransactionsInteract(),
                viewModel.getAssetDefinitionService(), this);
        SwipeRefreshLayout refreshLayout = view.findViewById(R.id.refresh_layout);
        systemView = view.findViewById(R.id.system_view);
        listView = view.findViewById(R.id.list);
        listView.setLayoutManager(new LinearLayoutManager(requireContext()));
        listView.setAdapter(adapter);
        listView.addRecyclerListener(holder -> adapter.onRViewRecycled(holder));

        systemView.attachRecyclerView(listView);
        systemView.attachSwipeRefreshLayout(refreshLayout);

        systemView.showProgress(false);
        refreshLayout.setOnRefreshListener(this::refreshTransactionList);
    }

    private void onDefaultWallet(Wallet wallet)
    {
        adapter.setDefaultWallet(wallet);
    }

    private void showEmptyTx()
    {
        if (adapter.isEmpty())
        {
            EmptyTransactionsView emptyView = new EmptyTransactionsView(requireContext(), this);
            systemView.showEmpty(emptyView);
        }
        else
        {
            systemView.hide();
        }
    }

    private void refreshTransactionList()
    {
        //clear tx list and reload
        adapter.clear();
        viewModel.prepare();
    }

    @Override
    public void resetTokens()
    {
        if (adapter != null)
        {
            //wallet changed, reset
            adapter.clear();
            viewModel.prepare();
        }
        else
        {
            requireActivity().recreate();
        }
    }

    @Override
    public void addedToken(List<ContractLocator> tokenContracts)
    {
        if (adapter != null) adapter.updateItems(tokenContracts);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (realmUpdates != null) realmUpdates.removeAllChangeListeners();
        if (realm != null && !realm.isClosed()) realm.close();
        if (viewModel != null) viewModel.onDestroy();
        if (adapter != null && listView != null) adapter.onDestroy(listView);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (viewModel == null)
        {
            requireActivity().recreate();
        }
        else
        {
            viewModel.track(Analytics.Navigation.ACTIVITY);
            viewModel.prepare();
        }

        checkTimer = true;
    }

    @Override
    public void fetchMoreData(long latestDate)
    {
        if (checkTimer)
        {
            viewModel.fetchMoreTransactions(latestDate);
            checkTimer = false;
            handler.postDelayed(() -> {
                checkTimer = true;
            }, 5 * DateUtils.SECOND_IN_MILLIS); //restrict checking for previous transactions every 5 seconds
        }
    }

    @Override
    public void onClick(View v)
    {

    }

    @Override
    public void comeIntoFocus()
    {
        isVisible = true;
        //start listener
        startTxListener(); //adjust for timestamp delay
    }

    @Override
    public void leaveFocus()
    {
        isVisible = false;
        //stop listener
        if (realmUpdates != null) realmUpdates.removeAllChangeListeners();
        if (realm != null && !realm.isClosed()) realm.close();
    }

    @Override
    public void resetTransactions()
    {
        //called when we just refreshed the database
        refreshTransactionList();
    }

    @Override
    public void scrollToTop()
    {
        if (listView != null) listView.smoothScrollToPosition(0);
    }
}
