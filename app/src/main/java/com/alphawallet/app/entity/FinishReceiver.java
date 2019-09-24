package com.alphawallet.app.entity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.alphawallet.app.C;

public class FinishReceiver extends BroadcastReceiver
{
    private final Activity activity;
    public FinishReceiver(Activity ctx)
    {
        activity = ctx;
        ctx.registerReceiver(this, new IntentFilter(C.PRUNE_ACTIVITY));
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent.getAction().equals(C.PRUNE_ACTIVITY))
            activity.finish();
    }
}
