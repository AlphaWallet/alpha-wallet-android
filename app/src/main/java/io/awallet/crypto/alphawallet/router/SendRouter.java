package io.awallet.crypto.alphawallet.router;

import android.content.Context;
import android.content.Intent;

import io.awallet.crypto.alphawallet.C;
import io.awallet.crypto.alphawallet.ui.SendActivity;

public class SendRouter {

    public void open(Context context, String symbol) {
        Intent intent = new Intent(context, SendActivity.class);
        intent.putExtra(C.EXTRA_SYMBOL, symbol);
        context.startActivity(intent);
    }
}
