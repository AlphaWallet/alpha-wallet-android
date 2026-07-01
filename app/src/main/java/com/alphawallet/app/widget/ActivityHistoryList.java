package com.alphawallet.app.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ActivityMeta;
import com.alphawallet.app.entity.TransactionMeta;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.entity.RealmTransaction;
import com.alphawallet.app.repository.entity.RealmTransfer;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.adapter.ActivityAdapter;
import com.alphawallet.app.ui.widget.entity.TokenTransferData;

import java.util.ArrayList;
import java.util.List;

import io.realm.kotlin.Realm;
import io.realm.kotlin.TypedRealm;
import com.alphawallet.app.realm.RealmCompat;
import com.alphawallet.app.realm.RealmQueryBuilder;
import com.alphawallet.app.realm.RealmCompat.Results;

/**
 * Created by JB on 5/08/2020.
 */
public class ActivityHistoryList extends LinearLayout
{
    private final RecyclerView recentTransactionsView;
    private final LinearLayout noTxNotice;
    private final ProgressBar loadingTransactions;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ActivityAdapter activityAdapter;
    private TypedRealm realm;
    private RealmCompat.Results<RealmTransaction> realmTransactionUpdates;
    private RealmQueryBuilder<RealmTransaction> realmUpdateQuery;

    public ActivityHistoryList(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.layout_activity_history, this);

        recentTransactionsView = findViewById(R.id.list);
        recentTransactionsView.setLayoutManager(new LinearLayoutManager(getContext()));
        loadingTransactions = findViewById(R.id.loading_transactions);
        noTxNotice = findViewById(R.id.layout_no_recent_transactions);
    }

    public void setupAdapter(ActivityAdapter adapter)
    {
        activityAdapter = adapter;
        recentTransactionsView.setAdapter(adapter);
    }

    public void startActivityListeners(TypedRealm realm, Wallet wallet, Token token, TokensService svs, final int historyCount)
    {
        this.realm = realm;

        activityAdapter.setItemLimit(historyCount);

        //handle updated realm transactions
        if (!token.isEthereum() || svs.isChainToken(token.tokenInfo.chainId, token.getAddress()))
        {
            realmUpdateQuery = getContractListener(token.tokenInfo.chainId, token.getAddress(), historyCount);
        }
        else
        {
            realmUpdateQuery = getEthListener(token.tokenInfo.chainId, wallet, historyCount);
        }

        if (realmTransactionUpdates != null) realmTransactionUpdates.removeAllChangeListeners();

        realmTransactionUpdates = realmUpdateQuery.findAllAsync();

        realmTransactionUpdates.addChangeListener(result -> handleRealmTransactions(result, wallet));
    }

    public boolean resetAdapter()
    {
        if (activityAdapter == null)
        {
            return false;
        }
        else
        {
            activityAdapter.clear();
            return true;
        }
    }

    private void handleRealmTransactions(RealmCompat.Results<RealmTransaction> realmTransactions, Wallet wallet)
    {
        boolean hasPending = false;
        List<ActivityMeta> metas = new ArrayList<>();
        for (RealmTransaction item : realmTransactions)
        {
            TransactionMeta tm = new TransactionMeta(item.getHash(), item.getTimeStamp(), item.getTo(), item.getChainId(), item.getBlockNumber());
            metas.add(tm);
            metas.addAll(getRelevantTransfersForHash(tm, wallet));
            if (tm.isPending) hasPending = true;
        }

        addItems(metas);

        if (hasPending)
        {
            handler.postDelayed(() ->
            {
                if (realm != null && !realm.isClosed()) //in theory this shouldn't happen because we remove all handler messages in onDestroy
                {
                    RealmCompat.refresh(realm);
                }
            }, 5000);
        }
    }

    private List<TokenTransferData> getRelevantTransfersForHash(TransactionMeta tm, Wallet wallet)
    {
        List<TokenTransferData> transferData = new ArrayList<>();
        //summon realm items
        //get matching entries for this transaction
        RealmCompat.Results<RealmTransfer> transfers = RealmCompat.where(realm, RealmTransfer.class)
                .equalTo("hash", RealmTransfer.databaseKey(tm.chainId, tm.hash))
                .findAll();

        if (transfers != null && transfers.size() > 0)
        {
            //list of transfers, descending in time to give ordered list
            long nextTransferTime = transfers.size() == 1 ? tm.getTimeStamp() : tm.getTimeStamp() - 1; // if there's only 1 transfer, keep the transaction timestamp
            for (RealmTransfer rt : transfers)
            {
                if (rt.getTransferDetail().contains(wallet.address))
                {
                    TokenTransferData ttd = new TokenTransferData(rt.getHash(), tm.chainId,
                            rt.getTokenAddress(), rt.getEventName(), rt.getTransferDetail(), nextTransferTime);
                    transferData.add(ttd);
                    nextTransferTime--;
                }
            }

            //For clarity, show only 1 item if it was part of a chain; ie don't show raw transaction
            if (transfers.size() > 1 && transferData.size() == 1)
            {
                TokenTransferData oldTf = transferData.get(0);
                transferData.clear();
                transferData.add(new TokenTransferData(oldTf.hash, tm.chainId, oldTf.tokenAddress, oldTf.eventName, oldTf.transferDetail, tm.getTimeStamp()));
            }
        }

        return transferData;
    }

    private RealmQueryBuilder<RealmTransaction> getContractListener(long chainId, String tokenAddress, int count)
    {
        return RealmCompat.where(realm, RealmTransaction.class)
                .sort("timeStamp", RealmCompat.Sort.DESCENDING)
                .beginGroup().not().equalTo("input", "0x").and()
                .beginGroup().equalTo("to", tokenAddress, RealmCompat.Case.INSENSITIVE)
                .or().equalTo("contractAddress", tokenAddress, RealmCompat.Case.INSENSITIVE).endGroup().endGroup()
                .equalTo("chainId", chainId)
                .limit(count);
    }

    private RealmQueryBuilder<RealmTransaction> getEthListener(long chainId, Wallet wallet, int count)
    {
        return RealmCompat.where(realm, RealmTransaction.class)
                .sort("timeStamp", RealmCompat.Sort.DESCENDING)
                .beginGroup()
                .equalTo("input", "0x").or().equalTo("input", "")
                .endGroup()
                .beginGroup()
                .equalTo("to", wallet.address, RealmCompat.Case.INSENSITIVE)
                .or()
                .equalTo("from", wallet.address, RealmCompat.Case.INSENSITIVE)
                .endGroup()
                .equalTo("chainId", chainId)
                .limit(count);
    }

    private void addItems(List<ActivityMeta> metas)
    {
        handler.post(() -> {
            loadingTransactions.setVisibility(View.GONE);
            if (metas.size() > 0 && !realm.isClosed())
            {
                activityAdapter.updateActivityItems(metas.toArray(new ActivityMeta[0]));
                recentTransactionsView.setVisibility(View.VISIBLE);
                noTxNotice.setVisibility(View.GONE);
            }
            else if (metas.size() == 0 && activityAdapter.getItemCount() == 0)
            {
                noTxNotice.setVisibility(View.VISIBLE);
            }
        });
    }

    public void onDestroy()
    {
        handler.removeCallbacksAndMessages(null);
        if (realmTransactionUpdates != null) realmTransactionUpdates.removeAllChangeListeners();
        if (realm != null && !RealmCompat.isClosed(realm)) RealmCompat.close(realm);
        if (activityAdapter != null && recentTransactionsView != null)
            activityAdapter.onDestroy(recentTransactionsView);
        realmUpdateQuery = null;
    }
}
