package io.stormbird.wallet.ui.widget;

import android.view.View;

import io.stormbird.wallet.entity.Transaction;

public interface OnTransactionClickListener {
    void onTransactionClick(View view, Transaction transaction);
}
