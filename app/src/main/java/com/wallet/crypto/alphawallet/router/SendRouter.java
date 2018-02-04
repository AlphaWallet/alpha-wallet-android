package com.wallet.crypto.alphawallet.router;

import android.content.Context;
import android.content.Intent;

import com.wallet.crypto.alphawallet.C;
import com.wallet.crypto.alphawallet.ui.SendActivity;

public class SendRouter {

    public void open(Context context, String symbol) {
        Intent intent = new Intent(context, SendActivity.class);
        intent.putExtra(C.EXTRA_SYMBOL, symbol);
        context.startActivity(intent);
    }
}
