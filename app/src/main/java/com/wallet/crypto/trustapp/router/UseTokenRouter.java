package com.wallet.crypto.trustapp.router;

import android.content.Context;
import android.content.Intent;

import com.wallet.crypto.trustapp.ui.AddTokenActivity;
import com.wallet.crypto.trustapp.ui.UseTokenActivity;

/**
 * Created by James on 22/01/2018.
 */

public class UseTokenRouter {

    public void open(Context context) {
        Intent intent = new Intent(context, UseTokenActivity.class);
        context.startActivity(intent);
    }
}
