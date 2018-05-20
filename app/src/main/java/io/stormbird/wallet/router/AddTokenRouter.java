package io.stormbird.wallet.router;

import android.content.Context;
import android.content.Intent;

import io.stormbird.wallet.ui.AddTokenActivity;

public class AddTokenRouter {

    public void open(Context context) {
        Intent intent = new Intent(context, AddTokenActivity.class);
        context.startActivity(intent);
    }
}
