package com.alphawallet.app.widget;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.ActivityMeta;
import com.alphawallet.app.entity.EventMeta;
import com.alphawallet.app.entity.TransactionMeta;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.entity.RealmAuxData;
import com.alphawallet.app.repository.entity.RealmTransaction;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.widget.adapter.ActivityAdapter;

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
import static com.alphawallet.app.repository.TokensRealmSource.EVENT_CARDS;

/**
 * Created by JB on 5/08/2020.
 */
public class ActivityHistoryList extends LinearLayout
{
    private ActivityAdapter activityAdapter;
    private Realm realm;
    private RealmResults<RealmTransaction> realmTransactionUpdates;
    private RealmQuery<RealmTransaction> realmUpdateQuery;
    private RealmResults<RealmAuxData> auxRealmUpdates;
    @Nullable private Disposable updateCheck; //performs a background check to ensure we get completion
    private final RecyclerView recentTransactionsView;
    private final LinearLayout noTxNotice;
    private final ProgressBar loadingTransactions;
    private final Handler handler = new Handler();
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
        if (auxRealmUpdates != null) auxRealmUpdates.removeAllChangeListeners();

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
        realmTransactionUpdates.addChangeListener(this::handleRealmTransactions);

        auxRealmUpdates = RealmAuxData.getEventListener(realm, token, tokenId, historyCount, 0);
        auxRealmUpdates.addChangeListener(realmEvents -> {
            List<ActivityMeta> metas = new ArrayList<>();
            for (RealmAuxData item : realmEvents)
            {
                EventMeta newMeta = new EventMeta(item.getTransactionHash(), item.getEventName(), item.getFunctionId(), item.getResultTime(), item.getChainId());
                metas.add(newMeta);
            }

            addItems(metas);
        });
    }

    private void handleRealmTransactions(RealmResults<RealmTransaction> realmTransactions)
    {
        boolean hasPending = false;
        List<ActivityMeta> metas = new ArrayList<>();
        for (RealmTransaction item : realmTransactions)
        {
            TransactionMeta tm = new TransactionMeta(item.getHash(), item.getTimeStamp(), item.getTo(), item.getChainId(), item.getBlockNumber());
            metas.add(tm);
            if (tm.isPending) hasPending = true;
        }

        addItems(metas);

        if (hasPending)
        {
            startUpdateCheck();
        }
        else
        {
            stopUpdateCheck();
        }
    }

    private void initViews(boolean isEth)
    {
        TextView noTransactionsSubText = findViewById(R.id.no_recent_transactions_subtext);

        if (isEth)
        {
            noTransactionsSubText.setText(context.getString(R.string.no_recent_transactions_subtext,
                    context.getString(R.string.no_recent_transactions_subtext_ether)));
        }
        else
        {
            noTransactionsSubText.setText(context.getString(R.string.no_recent_transactions_subtext,
                    context.getString(R.string.no_recent_transactions_subtext_tokens)));
        }
    }

    private RealmQuery<RealmTransaction> getContractListener(int chainId, String tokenAddress, int count)
    {
        return realm.where(RealmTransaction.class)
                .sort("timeStamp", Sort.DESCENDING)
                .beginGroup().not().equalTo("input", "0x").and().equalTo("to", tokenAddress, Case.INSENSITIVE).endGroup()
                .equalTo("chainId", chainId)
                .limit(count);
    }

    private RealmQuery<RealmTransaction> getEthListener(int chainId, Wallet wallet, int count)
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
        if (auxRealmUpdates != null) auxRealmUpdates.removeAllChangeListeners();
        if (realm != null && !realm.isClosed()) realm.close();
        handler.removeCallbacksAndMessages(null);
        stopUpdateCheck();
        if (activityAdapter != null && recentTransactionsView != null) activityAdapter.onDestroy(recentTransactionsView);
    }

    //Start update check on the database if anything is pending. Sometimes the listener doesn't pick up the change.
    private void startUpdateCheck()
    {
        if (updateCheck == null || updateCheck.isDisposed())
        {
            updateCheck = Observable.interval(0, 10, TimeUnit.SECONDS)
                    .doOnNext(l -> checkTransactions()).subscribe();
        }
    }

    private void stopUpdateCheck()
    {
        if (updateCheck != null && !updateCheck.isDisposed()) updateCheck.dispose();
        updateCheck = null;
    }

    private void checkTransactions()
    {
        if (realmUpdateQuery != null)
        {
            handler.post(() -> {
                RealmResults<RealmTransaction> rTx = realmUpdateQuery.findAll();
                handleRealmTransactions(rTx);
            });
        }
    }
}
