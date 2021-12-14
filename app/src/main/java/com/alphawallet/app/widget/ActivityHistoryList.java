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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by JB on 5/08/2020.
 */
public class ActivityHistoryList extends LinearLayout
{
    private ActivityAdapter activityAdapter;
    private Realm realm;
    private RealmResults<RealmTransaction> realmTransactionUpdates;
    private RealmQuery<RealmTransaction> realmUpdateQuery;
    @Nullable private Disposable updateCheck; //performs a background check to ensure we get completion
    private final RecyclerView recentTransactionsView;
    private final LinearLayout noTxNotice;
    private final ProgressBar loadingTransactions;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Context context;

    public ActivityHistoryList(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.layout_activity_history, this);

        recentTransactionsView = findViewById(R.id.list);
        recentTransactionsView.setLayoutManager(new LinearLayoutManager(getContext()));
        loadingTransactions = findViewById(R.id.loading_transactions);
        noTxNotice = findViewById(R.id.layout_no_recent_transactions);
        this.context = context;
    }

    public void setupAdapter(ActivityAdapter adapter)
    {
        activityAdapter = adapter;
        recentTransactionsView.setAdapter(adapter);
    }

    public void startActivityListeners(Realm realm, Wallet wallet, Token token, TokensService svs, BigInteger tokenId, final int historyCount)
    {
        this.realm = realm;

        activityAdapter.setItemLimit(historyCount);

        //stop any existing listeners (could be view refresh)
        if (realmTransactionUpdates != null) realmTransactionUpdates.removeAllChangeListeners();

        if (!token.isEthereum() || svs.isChainToken(token.tokenInfo.chainId, token.getAddress()))
        {
            realmUpdateQuery = getContractListener(token.tokenInfo.chainId, token.getAddress(), historyCount);
            initViews(false);
        }
        else
        {
            realmUpdateQuery = getEthListener(token.tokenInfo.chainId, wallet, historyCount);
            initViews(true);
        }

        realmTransactionUpdates = realmUpdateQuery.findAllAsync();

        //handle updated realm transactions
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

    private void handleRealmTransactions(RealmResults<RealmTransaction> realmTransactions, Wallet wallet)
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
            startUpdateCheck(wallet);
        }
        else
        {
            stopUpdateCheck();
        }
    }

    private List<TokenTransferData> getRelevantTransfersForHash(TransactionMeta tm, Wallet wallet)
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

    private void initViews(boolean isEth)
    {
//        TextView noTransactionsSubText = findViewById(R.id.no_recent_transactions_subtext);
//
//        if (isEth)
//        {
//            noTransactionsSubText.setText(context.getString(R.string.no_recent_transactions_subtext,
//                    context.getString(R.string.no_recent_transactions_subtext_ether)));
//        }
//        else
//        {
//            noTransactionsSubText.setText(context.getString(R.string.no_recent_transactions_subtext,
//                    context.getString(R.string.no_recent_transactions_subtext_tokens)));
//        }
    }

    private RealmQuery<RealmTransaction> getContractListener(long chainId, String tokenAddress, int count)
    {
        return realm.where(RealmTransaction.class)
                .sort("timeStamp", Sort.DESCENDING)
                .beginGroup().not().equalTo("input", "0x").and().equalTo("to", tokenAddress, Case.INSENSITIVE)
                             .or().equalTo("contractAddress", tokenAddress).endGroup()
                .equalTo("chainId", chainId)
                .limit(count);
    }

    private RealmQuery<RealmTransaction> getEthListener(long chainId, Wallet wallet, int count)
    {
        return realm.where(RealmTransaction.class)
                .sort("timeStamp", Sort.DESCENDING)
                .equalTo("input", "0x")
                .beginGroup()
                .equalTo("to", wallet.address, Case.INSENSITIVE)
                .or()
                .equalTo("from", wallet.address, Case.INSENSITIVE)
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
        if (realmTransactionUpdates != null) realmTransactionUpdates.removeAllChangeListeners();
        if (realm != null && !realm.isClosed()) realm.close();
        handler.removeCallbacksAndMessages(null);
        stopUpdateCheck();
        if (activityAdapter != null && recentTransactionsView != null) activityAdapter.onDestroy(recentTransactionsView);
    }

    //Start update check on the database if anything is pending. Sometimes the listener doesn't pick up the change.
    private void startUpdateCheck(final Wallet wallet)
    {
        if (updateCheck == null || updateCheck.isDisposed())
        {
            updateCheck = Observable.interval(0, 10, TimeUnit.SECONDS)
                    .doOnNext(l -> checkTransactions(wallet)).subscribe();
        }
    }

    private void stopUpdateCheck()
    {
        if (updateCheck != null && !updateCheck.isDisposed()) updateCheck.dispose();
        updateCheck = null;
    }

    private void checkTransactions(final Wallet wallet)
    {
        if (realmUpdateQuery != null)
        {
            handler.post(() -> {
                RealmResults<RealmTransaction> rTx = realmUpdateQuery.findAll();
                handleRealmTransactions(rTx, wallet);
            });
        }
    }
}
