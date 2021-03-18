package com.alphawallet.app.ui;

import android.os.Bundle;
import android.os.Handler;
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

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ActivityMeta;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.EventMeta;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionMeta;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletPage;
import com.alphawallet.app.interact.ActivityDataInteract;
import com.alphawallet.app.repository.entity.RealmAuxData;
import com.alphawallet.app.repository.entity.RealmTransaction;
import com.alphawallet.app.repository.entity.RealmTransfer;
import com.alphawallet.app.ui.widget.adapter.ActivityAdapter;
import com.alphawallet.app.ui.widget.adapter.RecycleViewDivider;
import com.alphawallet.app.ui.widget.entity.TokenTransferData;
import com.alphawallet.app.viewmodel.ActivityViewModel;
import com.alphawallet.app.viewmodel.ActivityViewModelFactory;
import com.alphawallet.app.widget.EmptyTransactionsView;
import com.alphawallet.app.widget.SystemView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import io.realm.Realm;
import io.realm.RealmResults;

import static com.alphawallet.app.repository.TokensRealmSource.EVENT_CARDS;

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
    private RealmResults<RealmAuxData> auxRealmUpdates;
    private String realmId;
    private long eventTimeFilter;
    private final Handler handler = new Handler();
    private boolean checkTimer;

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
            viewModel = new ViewModelProvider(this, activityViewModelFactory)
                    .get(ActivityViewModel.class);
            viewModel.defaultWallet().observe(getViewLifecycleOwner(), this::onDefaultWallet);
            viewModel.activityItems().observe(getViewLifecycleOwner(), this::onItemsLoaded);
        }
    }

    private void onItemsLoaded(ActivityMeta[] activityItems)
    {
        realm = viewModel.getRealmInstance();
        adapter.updateActivityItems(buildTransactionList(activityItems).toArray(new ActivityMeta[0]));
        showEmptyTx();
        long lastUpdateTime = 0;

        for (ActivityMeta am : activityItems)
        {
            if (am instanceof TransactionMeta && am.getTimeStampSeconds() > lastUpdateTime) lastUpdateTime = am.getTimeStampSeconds();
        }

        startTxListener(lastUpdateTime - 60*10); //adjust for timestamp delay
    }

    private void startTxListener(long lastUpdateTime)
    {
        String walletAddress = viewModel.defaultWallet().getValue() != null ? viewModel.defaultWallet().getValue().address : "";
        eventTimeFilter = lastUpdateTime;
        if (realmId == null || !realmId.equalsIgnoreCase(walletAddress))
        {
            if (realmUpdates != null) realmUpdates.removeAllChangeListeners();

            realmId = walletAddress;
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
                    }
                }

                if (metas.size() > 0)
                {
                    TransactionMeta[] metaArray = metas.toArray(new TransactionMeta[0]);
                    adapter.updateActivityItems(buildTransactionList(metaArray).toArray(new ActivityMeta[0]));
                    systemView.hide();
                }
            });

            auxRealmUpdates = realm.where(RealmAuxData.class)
                    .endsWith("instanceKey", EVENT_CARDS)
                    .greaterThan("resultReceivedTime", lastUpdateTime)
                    .findAllAsync();
            auxRealmUpdates.addChangeListener(realmEvents -> {
                List<ActivityMeta> metas = new ArrayList<>();
                if (realmEvents.size() == 0) return;
                for (RealmAuxData item : realmEvents)
                {
                    if (item.getResultReceivedTime() >= eventTimeFilter && viewModel.getTokensService().getNetworkFilters().contains(item.getChainId()))
                    {
                        EventMeta newMeta = new EventMeta(item.getTransactionHash(), item.getEventName(), item.getFunctionId(), item.getResultTime(), item.getChainId());
                        metas.add(newMeta);
                    }
                }

                eventTimeFilter = System.currentTimeMillis() - DateUtils.SECOND_IN_MILLIS; // allow for async; may receive many event updates

                if (metas.size() > 0)
                {
                    adapter.updateActivityItems(metas.toArray(new ActivityMeta[0]));
                    systemView.hide();
                }
            });
        }
    }

    private List<ActivityMeta> buildTransactionList(ActivityMeta[] activityItems)
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
                List<TokenTransferData> tokenTransfers = getTokenTransfersForHash((TransactionMeta)am);
                if (tokenTransfers.size() != 1) { filteredList.add(am); } //only 1 token transfer ? No need to show the underlying transaction
                filteredList.addAll(tokenTransfers);
            }
        }

        return filteredList;
    }

    private List<TokenTransferData> getTokenTransfersForHash(TransactionMeta tm)
    {
        List<TokenTransferData> transferData = new ArrayList<>();
        //summon realm items
        //get matching entries for this transaction
        RealmResults<RealmTransfer> transfers = realm.where(RealmTransfer.class)
                .equalTo("hash", tm.hash)
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
        listView.setLayoutManager(new LinearLayoutManager(getContext()));
        listView.setAdapter(adapter);
        listView.addItemDecoration(new RecycleViewDivider(getContext()));
        listView.setRecyclerListener(holder -> adapter.onRViewRecycled(holder));

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
            EmptyTransactionsView emptyView = new EmptyTransactionsView(getContext(), this);
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

    public void resetTokens()
    {
        if (adapter != null)
        {
            //wallet changed, reset
            adapter.clear();
            viewModel.prepare();
        }
    }

    public void addedToken(List<ContractLocator> tokenContracts)
    {
        if (adapter != null) adapter.updateItems(tokenContracts);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (realmUpdates != null) realmUpdates.removeAllChangeListeners();
        if (auxRealmUpdates != null) auxRealmUpdates.removeAllChangeListeners();
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
            ((HomeActivity)getActivity()).resetFragment(WalletPage.ACTIVITY);
        }
        else
        {
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
            }, 5*DateUtils.SECOND_IN_MILLIS); //restrict checking for previous transactions every 5 seconds
        }
    }

    @Override
    public void onClick(View v)
    {

    }

    public void resetTransactions()
    {
        //called when we just refreshed the database
        refreshTransactionList();
    }
}
