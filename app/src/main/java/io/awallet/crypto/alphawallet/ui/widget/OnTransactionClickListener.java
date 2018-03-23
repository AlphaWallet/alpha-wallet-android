package io.awallet.crypto.alphawallet.ui.widget;

import android.view.View;

import io.awallet.crypto.alphawallet.entity.Transaction;

public interface OnTransactionClickListener {
    void onTransactionClick(View view, Transaction transaction);
}
