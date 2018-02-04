package com.wallet.crypto.trustapp.ui.widget;

import android.view.View;

import com.wallet.crypto.trustapp.entity.Transaction;

public interface OnTransactionClickListener {
    void onTransactionClick(View view, Transaction transaction);
}
