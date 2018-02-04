package com.wallet.crypto.alphawallet.ui.widget;

import android.view.View;

import com.wallet.crypto.alphawallet.entity.Transaction;

public interface OnTransactionClickListener {
    void onTransactionClick(View view, Transaction transaction);
}
