package com.wallet.crypto.trustapp.router;

import android.content.Context;
import android.content.Intent;

import com.wallet.crypto.trustapp.C;
import com.wallet.crypto.trustapp.ui.SendActivity;

public class SendRouter {

    public void open(Context context, String symbol) {
        Intent intent = new Intent(context, SendActivity.class);
        intent.putExtra(C.EXTRA_SYMBOL, symbol);
        context.startActivity(intent);
    }
}
