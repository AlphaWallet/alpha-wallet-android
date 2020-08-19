package com.alphawallet.app.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import io.realm.RealmChangeListener;
import io.realm.RealmModel;
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
    private int lastPos;

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
        eventTimeFilter = lastUpdateTime;
        if (realmId == null || !realmId.equals(walletAddress))
        {
            if (realmUpdates != null) realmUpdates.removeAllChangeListeners();

            realmId = walletAddress;
            realm = viewModel.getRealmInstance();
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
        adapter.updateItems(tokenContracts);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (realmUpdates != null) realmUpdates.removeAllChangeListeners();
        if (auxRealmUpdates != null) auxRealmUpdates.removeAllChangeListeners();
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

        checkTimer = true;
        lastPos = 0;
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
        //open exchange dialog
        ((HomeActivity)getActivity()).openExchangeDialog();
    }

    public void resetTransactions()
    {
        //called when we just refreshed the database
        refreshTransactionList();
    }
}
