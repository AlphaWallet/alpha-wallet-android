package io.awallet.crypto.alphawallet.router;


import android.content.Context;
import android.content.Intent;

import io.awallet.crypto.alphawallet.ui.EthereumInfoActivity;
import io.awallet.crypto.alphawallet.ui.HelpActivity;

public class EthereumInfoRouter {
    public void open(Context context) {
        Intent intent = new Intent(context, EthereumInfoActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }
}
