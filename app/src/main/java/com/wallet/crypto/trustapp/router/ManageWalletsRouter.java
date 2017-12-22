package com.wallet.crypto.trustapp.router;

import android.content.Context;
import android.content.Intent;

import com.wallet.crypto.trustapp.ui.ManageWalletsActivity;

public class ManageWalletsRouter {

    public void open(Context context) {
        Intent intent = new Intent(context, ManageWalletsActivity.class);
        context.startActivity(intent);
    }
}
