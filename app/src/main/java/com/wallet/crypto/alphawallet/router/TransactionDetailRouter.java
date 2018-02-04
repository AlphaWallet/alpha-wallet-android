package com.wallet.crypto.trustapp.router;

import android.content.Context;
import android.content.Intent;

import com.wallet.crypto.trustapp.entity.Transaction;
import com.wallet.crypto.trustapp.ui.TransactionDetailActivity;

import static com.wallet.crypto.trustapp.C.Key.TRANSACTION;

public class TransactionDetailRouter {

    public void open(Context context, Transaction transaction) {
        Intent intent = new Intent(context, TransactionDetailActivity.class);
        intent.putExtra(TRANSACTION, transaction);
        context.startActivity(intent);
    }
}
