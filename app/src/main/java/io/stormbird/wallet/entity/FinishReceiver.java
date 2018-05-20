package io.stormbird.wallet.entity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import static io.stormbird.wallet.C.PRUNE_ACTIVITY;

public class FinishReceiver extends BroadcastReceiver
{
    private final Activity activity;
    public FinishReceiver(Activity ctx)
    {
        activity = ctx;
        ctx.registerReceiver(this, new IntentFilter(PRUNE_ACTIVITY));
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent.getAction().equals(PRUNE_ACTIVITY))
            activity.finish();
    }
}
