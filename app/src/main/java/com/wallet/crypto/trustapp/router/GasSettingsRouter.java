package com.wallet.crypto.trustapp.router;


import android.content.Context;
import android.content.Intent;

import com.wallet.crypto.trustapp.ui.GasSettingsActivity;

public class GasSettingsRouter {
    public void open(Context context) {
        Intent intent = new Intent(context, GasSettingsActivity.class);
        context.startActivity(intent);
    }
}
