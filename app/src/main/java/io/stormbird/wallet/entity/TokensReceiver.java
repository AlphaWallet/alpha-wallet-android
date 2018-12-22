package io.stormbird.wallet.entity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import static io.stormbird.wallet.C.ADDED_TOKEN;
import static io.stormbird.wallet.C.CHANGED_LOCALE;
import static io.stormbird.wallet.C.RESET_WALLET;

public class TokensReceiver extends BroadcastReceiver
{
    private final TokenInterface tokenInterface;
    public TokensReceiver(Activity ctx, TokenInterface tokenInterface)
    {
        ctx.registerReceiver(this, new IntentFilter(RESET_WALLET));
        ctx.registerReceiver(this, new IntentFilter(ADDED_TOKEN));
        ctx.registerReceiver(this, new IntentFilter(CHANGED_LOCALE));
        this.tokenInterface = tokenInterface;
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent != null && intent.getAction() != null)
        {
            switch (intent.getAction())
            {
                case RESET_WALLET:
                    tokenInterface.resetTokens();
                    break;
                case ADDED_TOKEN:
                    tokenInterface.addedToken();
                    break;
                case CHANGED_LOCALE:
                    tokenInterface.changedLocale();
                    break;
                default:
                    break;
            }
        }
    }
}
