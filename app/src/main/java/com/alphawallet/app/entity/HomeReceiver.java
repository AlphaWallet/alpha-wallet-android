package com.alphawallet.app.entity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.alphawallet.app.C;

public class HomeReceiver extends BroadcastReceiver
{
    private final HomeCommsInterface homeCommsInterface;
    public HomeReceiver(Activity ctx, HomeCommsInterface homeCommsInterface)
    {
        ctx.registerReceiver(this, new IntentFilter(C.DOWNLOAD_READY));
        ctx.registerReceiver(this, new IntentFilter(C.REQUEST_NOTIFICATION_ACCESS));
        ctx.registerReceiver(this, new IntentFilter(C.BACKUP_WALLET_SUCCESS));
        ctx.registerReceiver(this, new IntentFilter(C.WALLET_CONNECT_REQUEST));
        this.homeCommsInterface = homeCommsInterface;
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Bundle bundle = intent.getExtras();
        switch (intent.getAction())
        {
            case C.DOWNLOAD_READY:
                String message = bundle.getString("Version");
                homeCommsInterface.downloadReady(message);
                break;
            case C.REQUEST_NOTIFICATION_ACCESS:
                homeCommsInterface.requestNotificationPermission();
                break;
            case C.BACKUP_WALLET_SUCCESS:
                String keyAddress = bundle.getString("Key");
                homeCommsInterface.backupSuccess(keyAddress);
                break;
            case C.WALLET_CONNECT_REQUEST:
                String sessionId = bundle.getString("sessionid");
                homeCommsInterface.openWalletConnect(sessionId);
            default:
                break;
        }
    }
}
