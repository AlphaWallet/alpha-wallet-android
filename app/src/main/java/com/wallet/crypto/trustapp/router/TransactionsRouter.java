package com.wallet.crypto.trustapp.router;

import android.content.Context;
import android.content.Intent;

import com.wallet.crypto.trustapp.ui.TransactionsActivity;

public class TransactionsRouter {
    public void open(Context context) {
        Intent intent = new Intent(context, TransactionsActivity.class);
        context.startActivity(intent);
    }
}
