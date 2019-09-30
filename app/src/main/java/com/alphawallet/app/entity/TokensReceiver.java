package com.alphawallet.app.entity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.alphawallet.app.C;

public class TokensReceiver extends BroadcastReceiver
{
    private final TokenInterface tokenInterface;
    public TokensReceiver(Activity ctx, TokenInterface tokenInterface)
    {
        ctx.registerReceiver(this, new IntentFilter(C.RESET_WALLET));
        ctx.registerReceiver(this, new IntentFilter(C.ADDED_TOKEN));
        ctx.registerReceiver(this, new IntentFilter(C.CHANGED_LOCALE));
        this.tokenInterface = tokenInterface;
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent != null && intent.getAction() != null)
        {
            switch (intent.getAction())
            {
                case C.RESET_WALLET:
                    tokenInterface.resetTokens();
                    break;
                case C.ADDED_TOKEN:
                    tokenInterface.addedToken();
                    break;
                case C.CHANGED_LOCALE:
                    tokenInterface.changedLocale();
                    break;
                default:
                    break;
            }
        }
    }
}
