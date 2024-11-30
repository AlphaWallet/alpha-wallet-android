package com.alphawallet.app.entity;

import static androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED;
import static androidx.core.content.ContextCompat.registerReceiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

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
        register(ctx);
    }

    private void register(Activity ctx)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            registerReceiver(ctx, this, new IntentFilter(C.WALLET_CONNECT_COUNT_CHANGE), RECEIVER_NOT_EXPORTED);
        }
        else
        {
            broadcastManager.registerReceiver(this, new IntentFilter(C.PRUNE_ACTIVITY));
        }
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        activity.finish();
    }

    public void unregister()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            activity.unregisterReceiver(this);
        }
        else
        {
            broadcastManager.unregisterReceiver(this);
        }
    }
}
