package com.alphawallet.app.widget;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.repository.TransactionsRealmCache;
import com.alphawallet.app.repository.entity.RealmTransaction;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by JB on 27/11/2020.
 */
public class ConfirmationWidget extends RelativeLayout
{
    private final ProgressKnobkerry progress;
    private final TextView hashText;
    private final RelativeLayout progressLayout;
    private RealmResults<RealmTransaction> realmTransactionUpdates;
    private final Handler handler = new Handler();

    public ConfirmationWidget(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        inflate(context, R.layout.item_confirmation, this);
        progress = findViewById(R.id.progress_knob);
        progressLayout = findViewById(R.id.layout_confirmation);
        hashText = findViewById(R.id.hash_text);
    }

    public void startAnimate(long expectedTransactionTime, Realm transactionRealm, String txHash)
    {
        progress.setVisibility(View.VISIBLE);
        progressLayout.setVisibility(View.VISIBLE);
        progress.startAnimation(expectedTransactionTime);
        createCompletionListener(transactionRealm, txHash);
        if (!TextUtils.isEmpty(txHash))
        {
            hashText.setVisibility(View.VISIBLE);
            hashText.setAlpha(1.0f);
            hashText.setText(txHash);
            hashText.animate().setStartDelay(2000).alpha(0.0f).setDuration(1500);
        }
    }

    //listen for transaction completion
    private void createCompletionListener(Realm realm, String txHash)
    {
        realmTransactionUpdates = realm.where(RealmTransaction.class)
                .equalTo("hash", txHash)
                .findAllAsync();

        realmTransactionUpdates.addChangeListener(realmTransactions -> {
            if (realmTransactions.size() > 0)
            {
                RealmTransaction rTx = realmTransactions.first();
                if (rTx != null && !rTx.isPending())
                {
                    Transaction tx = TransactionsRealmCache.convert(rTx);
                    //tx written, update icon
                    handler.post(this::completeProgressSuccess);
                }
            }
        });
    }

    private void completeProgressSuccess()
    {
        realmTransactionUpdates.removeAllChangeListeners();
        progress.setVisibility(View.VISIBLE);
        progressLayout.setVisibility(View.VISIBLE);
        progress.setComplete();
    }

    public void showAnimate()
    {
        progress.setVisibility(View.VISIBLE);
        progressLayout.setVisibility(View.VISIBLE);
        progress.waitCycle();
    }

    public void hide()
    {
        progressLayout.setVisibility(View.GONE);
    }
}
