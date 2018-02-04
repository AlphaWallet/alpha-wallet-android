package com.wallet.crypto.alphawallet.router;

import android.content.Context;
import android.content.Intent;

import com.wallet.crypto.alphawallet.entity.Transaction;
import com.wallet.crypto.alphawallet.ui.TransactionDetailActivity;

import static com.wallet.crypto.alphawallet.C.Key.TRANSACTION;

public class TransactionDetailRouter {

    public void open(Context context, Transaction transaction) {
        Intent intent = new Intent(context, TransactionDetailActivity.class);
        intent.putExtra(TRANSACTION, transaction);
        context.startActivity(intent);
    }
}
