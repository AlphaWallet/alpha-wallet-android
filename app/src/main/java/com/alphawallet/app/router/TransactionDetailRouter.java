package com.alphawallet.app.router;

import android.content.Context;
import android.content.Intent;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.ui.TransactionDetailActivity;
import com.alphawallet.app.entity.Transaction;

public class TransactionDetailRouter {

    public void open(Context context, Transaction transaction, Wallet wallet) {
        Intent intent = new Intent(context, TransactionDetailActivity.class);
        intent.putExtra(C.Key.TRANSACTION, transaction);
        intent.putExtra(C.Key.WALLET, wallet);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivity(intent);
    }
}
