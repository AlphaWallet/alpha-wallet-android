package com.alphawallet.app.router;

import android.app.Activity;
import android.content.Intent;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.ui.SendActivity;

public class SendTokenRouter
{

    // From token detail page
    public void open(Activity context, Token token)
    {
        Intent intent = new Intent(context, SendActivity.class);
        intent.putExtra(C.EXTRA_CONTRACT_ADDRESS, token.getAddress());
        intent.putExtra(C.EXTRA_NETWORKID, token.tokenInfo.chainId);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivityForResult(intent, C.COMPLETED_TRANSACTION);
    }

    // From address scan
    public void open(Activity context, String recipientAddress)
    {
        //TODO Implement
        Intent intent = new Intent(context, SendActivity.class);
        intent.putExtra(C.EXTRA_ADDRESS, recipientAddress);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivityForResult(intent, C.COMPLETED_TRANSACTION);
    }

    // From bottom nav
    public void open(Activity context)
    {
        Intent intent = new Intent(context, SendActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivityForResult(intent, C.COMPLETED_TRANSACTION);
    }
}
