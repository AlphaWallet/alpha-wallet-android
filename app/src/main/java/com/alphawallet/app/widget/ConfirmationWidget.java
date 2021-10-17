package com.alphawallet.app.widget;

import android.animation.Animator;
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
import com.alphawallet.app.ui.widget.entity.ProgressCompleteCallback;

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
        realmTransactionUpdates = null;
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

    public void startProgressCycle(int cycleTime)
    {
        progress.setVisibility(View.VISIBLE);
        progressLayout.setVisibility(View.VISIBLE);
        progress.startAnimation(cycleTime);
    }

    public void completeProgressMessage(String message, final ProgressCompleteCallback callback)
    {
        Animator.AnimatorListener animatorListener = new Animator.AnimatorListener()
        {
            @Override
            public void onAnimationStart(Animator animation) { }

            @Override
            public void onAnimationEnd(Animator animation) { callback.progressComplete(); }

            @Override
            public void onAnimationCancel(Animator animation) { }

            @Override
            public void onAnimationRepeat(Animator animation) { }
        };

        if (!TextUtils.isEmpty(message))
        {
            completeProgressSuccess(true);
            hashText.setVisibility(View.VISIBLE);
            hashText.setAlpha(1.0f);
            if (message.length() > 1) hashText.setText(message);

            hashText.animate()
                    .alpha(0.0f)
                    .setDuration(1500)
                    .setListener(animatorListener);
        }
        else
        {
            completeProgressSuccess(false);
            hashText.setVisibility(View.GONE);
            hashText.animate()
                    .alpha(0.0f)
                    .setDuration(1500)
                    .setListener(animatorListener);
        }
    }

    //listen for transaction completion
    private void createCompletionListener(Realm realm, String txHash)
    {
        if (realmTransactionUpdates != null) realmTransactionUpdates.removeAllChangeListeners();

        realmTransactionUpdates = realm.where(RealmTransaction.class)
                .equalTo("hash", txHash)
                .findAllAsync();

        realmTransactionUpdates.addChangeListener(realmTransactions -> {
            if (realmTransactions.size() > 0)
            {
                RealmTransaction rTx = realmTransactions.first();
                if (rTx != null && !rTx.isPending())
                {
                    final Transaction tx = TransactionsRealmCache.convert(rTx);
                    //tx written, update icon
                    handler.post(() -> completeProgressSuccess(!tx.hasError()));
                }
            }
        });
    }

    private void completeProgressSuccess(boolean success)
    {
        if (realmTransactionUpdates != null) realmTransactionUpdates.removeAllChangeListeners();
        progress.setVisibility(View.VISIBLE);
        progressLayout.setVisibility(View.VISIBLE);
        progress.setComplete(success);
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
