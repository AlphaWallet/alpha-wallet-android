package com.wallet.crypto.alphawallet.router;

import android.content.Context;
import android.content.Intent;

import com.wallet.crypto.alphawallet.ui.TransactionsActivity;

public class TransactionsRouter {
    public void open(Context context, boolean isClearStack) {
        Intent intent = new Intent(context, TransactionsActivity.class);
        if (isClearStack) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        context.startActivity(intent);
    }
}
