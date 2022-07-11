package com.alphawallet.app.entity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.alphawallet.app.C;

public class FinishReceiver extends BroadcastReceiver
{
    private final Activity activity;
    private final LocalBroadcastManager broadcastManager;

    public FinishReceiver(Activity ctx)
    {
        activity = ctx;
        broadcastManager = LocalBroadcastManager.getInstance(ctx);
        register();
    }

    private void register()
    {
        broadcastManager.registerReceiver(this, new IntentFilter(C.PRUNE_ACTIVITY));
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        activity.finish();
    }

    public void unregister()
    {
        broadcastManager.unregisterReceiver(this);
    }
}
