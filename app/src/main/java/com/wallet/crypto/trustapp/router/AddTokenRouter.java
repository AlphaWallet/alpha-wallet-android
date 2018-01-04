package com.wallet.crypto.trustapp.router;

import android.content.Context;
import android.content.Intent;

import com.wallet.crypto.trustapp.ui.AddTokenActivity;

public class AddTokenRouter {

    public void open(Context context) {
        Intent intent = new Intent(context, AddTokenActivity.class);
        context.startActivity(intent);
    }
}
