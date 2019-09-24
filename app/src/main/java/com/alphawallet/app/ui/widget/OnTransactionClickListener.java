package com.alphawallet.app.ui.widget;

import android.view.View;

import com.alphawallet.app.entity.Transaction;

public interface OnTransactionClickListener {
    void onTransactionClick(View view, Transaction transaction);
}
