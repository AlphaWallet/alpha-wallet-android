package com.alphawallet.app.entity.tokens;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.alphawallet.app.C;

public class TokensReceiver extends BroadcastReceiver
{
    private final TokenInterface tokenInterface;
    public TokensReceiver(Activity ctx, TokenInterface tokenInterface)
    {
        ctx.registerReceiver(this, new IntentFilter(C.RESET_WALLET));
        ctx.registerReceiver(this, new IntentFilter(C.ADDED_TOKEN));
        ctx.registerReceiver(this, new IntentFilter(C.CHANGED_LOCALE));
        ctx.registerReceiver(this, new IntentFilter(C.REFRESH_TOKENS));
        this.tokenInterface = tokenInterface;
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent != null && intent.getAction() != null)
        {
            Bundle bundle = intent.getExtras();
            switch (intent.getAction())
            {
                case C.RESET_WALLET:
                    tokenInterface.resetTokens();
                    break;
                case C.ADDED_TOKEN:
                    String[] addrs = intent.getStringArrayExtra(C.EXTRA_TOKENID_LIST);
                    int[] chainIds = intent.getIntArrayExtra(C.EXTRA_CHAIN_ID);
                    tokenInterface.addedToken(chainIds, addrs);
                    break;
                case C.CHANGED_LOCALE:
                    tokenInterface.changedLocale();
                    break;
                case C.REFRESH_TOKENS:
                    tokenInterface.refreshTokens(); //only refresh tokens in wallet view
                    break;
                default:
                    break;
            }
        }
    }
}
