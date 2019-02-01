package io.stormbird.wallet.router;


import android.content.Context;
import android.content.Intent;

import io.stormbird.wallet.ui.EthereumInfoActivity;

public class EthereumInfoRouter {
    public void open(Context context) {
        Intent intent = new Intent(context, EthereumInfoActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }
}
