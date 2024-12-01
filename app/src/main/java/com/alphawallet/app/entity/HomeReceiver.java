package com.alphawallet.app.entity;

import static androidx.core.content.ContextCompat.registerReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.alphawallet.app.C;

public class HomeReceiver extends BroadcastReceiver
{
    private final HomeCommsInterface homeCommsInterface;
    private final LocalBroadcastManager broadcastManager;

    public HomeReceiver(Context context, HomeCommsInterface homeCommsInterface)
    {
        broadcastManager = LocalBroadcastManager.getInstance(context);
        this.homeCommsInterface = homeCommsInterface;
    }

    public HomeReceiver()
    {
        homeCommsInterface = null;
        broadcastManager = null;
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Bundle bundle = intent.getExtras();
        if (intent.getAction() != null)
        {
            switch (intent.getAction())
            {
                case C.REQUEST_NOTIFICATION_ACCESS:
                    homeCommsInterface.requestNotificationPermission();
                    break;
                case C.BACKUP_WALLET_SUCCESS:
                    String keyAddress = bundle != null ? bundle.getString("Key", "") : "";
                    homeCommsInterface.backupSuccess(keyAddress);
                    break;
                default:
                    break;
            }
        }
    }

    public void register(Context ctx)
    {
        IntentFilter filter = new IntentFilter();
        filter.addAction(C.REQUEST_NOTIFICATION_ACCESS);
        filter.addAction(C.BACKUP_WALLET_SUCCESS);
        filter.addAction(C.WALLET_CONNECT_REQUEST);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            registerReceiver(ctx, this, new IntentFilter(C.WALLET_CONNECT_COUNT_CHANGE), ContextCompat.RECEIVER_NOT_EXPORTED);
        }
        else
        {
            broadcastManager.registerReceiver(this, filter);
        }
    }

    public void unregister(Context ctx)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        {
            ctx.unregisterReceiver(this);
        }
        else
        {
            broadcastManager.unregisterReceiver(this);
        }
    }
}
