package io.stormbird.wallet.router;

import android.content.Context;
import android.content.Intent;

import io.stormbird.wallet.C;
import io.stormbird.wallet.ui.SendActivity;

public class SendRouter {

    public void open(Context context, String symbol) {
        Intent intent = new Intent(context, SendActivity.class);
        intent.putExtra(C.EXTRA_SYMBOL, symbol);
        context.startActivity(intent);
    }
}
