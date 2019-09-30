package com.alphawallet.app.router;

import android.content.Context;
import android.content.Intent;

import com.alphawallet.app.C;
import com.alphawallet.app.ui.AssetDisplayActivity;
import com.alphawallet.app.entity.Token;

/**
 * Created by James on 22/01/2018.
 */

public class AssetDisplayRouter {

    public void open(Context context, Token ticket) {
        Intent intent = new Intent(context, AssetDisplayActivity.class);
        intent.putExtra(C.Key.TICKET, ticket);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivity(intent);
    }

    public void open(Context context, Token ticket, boolean isClearStack) {
        Intent intent = new Intent(context, AssetDisplayActivity.class);
        if (isClearStack) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        else
        {
            intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        }
        intent.putExtra(C.Key.TICKET, ticket);
        context.startActivity(intent);
    }
}
