package com.alphawallet.app.router;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.ui.AssetDisplayActivity;
import com.alphawallet.app.entity.tokens.Token;

/**
 * Created by James on 22/01/2018.
 */

public class AssetDisplayRouter {

    public void open(Activity activity, Token ticket, Wallet wallet) {
        Intent intent = new Intent(activity, AssetDisplayActivity.class);
        intent.putExtra(C.Key.TICKET, ticket);
        intent.putExtra(C.Key.WALLET, wallet);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        activity.startActivityForResult(intent, C.TERMINATE_ACTIVITY);
    }
}
