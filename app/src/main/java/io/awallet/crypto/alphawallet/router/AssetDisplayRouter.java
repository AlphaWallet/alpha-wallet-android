package io.awallet.crypto.alphawallet.router;

import android.content.Context;
import android.content.Intent;

import io.awallet.crypto.alphawallet.entity.Token;
import io.awallet.crypto.alphawallet.ui.AssetDisplayActivity;

import static io.awallet.crypto.alphawallet.C.Key.TICKET;

/**
 * Created by James on 22/01/2018.
 */

public class AssetDisplayRouter {

    public void open(Context context, Token ticket) {
        Intent intent = new Intent(context, AssetDisplayActivity.class);
        intent.putExtra(TICKET, ticket);
        context.startActivity(intent);
    }

    public void open(Context context, Token ticket, boolean isClearStack) {
        Intent intent = new Intent(context, AssetDisplayActivity.class);
        if (isClearStack) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        intent.putExtra(TICKET, ticket);
        context.startActivity(intent);
    }
}
