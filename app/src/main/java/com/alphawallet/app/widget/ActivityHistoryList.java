package com.alphawallet.app.widget;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ActivityMeta;
import com.alphawallet.app.entity.EventMeta;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionMeta;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.repository.entity.RealmAuxData;
import com.alphawallet.app.repository.entity.RealmTransaction;
import com.alphawallet.app.ui.widget.adapter.ActivityAdapter;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

import static com.alphawallet.app.repository.TransactionsRealmCache.convert;

/**
 * Created by JB on 5/08/2020.
 */
public class ActivityHistoryList extends ScrollView
{
    private ActivityAdapter activityAdapter;
    private Realm realm;
    private RealmResults<RealmTransaction> realmTransactionUpdates;
    private RealmResults<RealmAuxData> auxRealmUpdates;
    private final RecyclerView recentTransactionsView;
    private final LinearLayout noTxNotice;
    private final Handler handler = new Handler();

    public ActivityHistoryList(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.layout_activity_history, this);

        recentTransactionsView = findViewById(R.id.list);
        recentTransactionsView.setLayoutManager(new LinearLayoutManager(getContext()));
        noTxNotice = findViewById(R.id.layout_no_recent_transactions);
        noTxNotice.setVisibility(View.VISIBLE);
    }

    public void setupAdapter(ActivityAdapter adapter)
    {
        activityAdapter = adapter;
        recentTransactionsView.setAdapter(adapter);
    }

    public void startActivityListeners(Realm realm, Wallet wallet, int chainId, final String tokenAddress, final int historyCount)
    {
        this.realm = realm;

        activityAdapter.setItemLimit(historyCount);

        //stop any existing listeners (could be view refresh)
        if (realmTransactionUpdates != null) realmTransactionUpdates.removeAllChangeListeners();
        if (auxRealmUpdates != null) auxRealmUpdates.removeAllChangeListeners();

        //start listeners
        realmTransactionUpdates = realm.where(RealmTransaction.class)
                .sort("timeStamp", Sort.DESCENDING)
                .equalTo("chainId",chainId)
                .findAllAsync();
        realmTransactionUpdates.addChangeListener(realmTransactions -> {
            List<ActivityMeta> metas = new ArrayList<>();
            //make list
            if (realmTransactions.size() == 0) return;
            for (RealmTransaction item : realmTransactions)
            {
                Transaction tx = convert(item);
                if (tx.isRelated(tokenAddress, wallet.address))
                {
                    TransactionMeta tm = new TransactionMeta(item.getHash(), item.getTimeStamp(), item.getTo(), item.getChainId(), item.getBlockNumber().equals("0"));
                    metas.add(tm);
                    if (metas.size() >= historyCount) break;
                }
            }

            addItems(metas);
        });

        auxRealmUpdates = realm.where(RealmAuxData.class)
                .endsWith("instanceKey", "-eventName")
                .sort("resultTime", Sort.DESCENDING)
                .equalTo("chainId", chainId)
                .equalTo("tokenAddress", tokenAddress)
                .findAllAsync();
        auxRealmUpdates.addChangeListener(realmEvents -> {
            List<ActivityMeta> metas = new ArrayList<>();
            if (realmEvents.size() == 0) return;
            for (RealmAuxData item : realmEvents)
            {
                EventMeta newMeta = new EventMeta(item.getTransactionHash(), item.getEventName(), item.getFunctionId(), item.getResultTime(), item.getChainId());
                metas.add(newMeta);
                if (metas.size() >= historyCount) break;
            }

            addItems(metas);
        });
    }

    private void addItems(List<ActivityMeta> metas)
    {
        handler.post(() -> {
            if (metas.size() > 0 && !realm.isClosed())
            {
                activityAdapter.updateActivityItems(metas.toArray(new ActivityMeta[0]));
                noTxNotice.setVisibility(View.GONE);
                recentTransactionsView.setVisibility(View.VISIBLE);
            }
        });
    }

    public void onDestroy()
    {
        if (realmTransactionUpdates != null) realmTransactionUpdates.removeAllChangeListeners();
        if (realm != null && !realm.isClosed()) realm.close();
        handler.removeCallbacksAndMessages(null);
    }
}
