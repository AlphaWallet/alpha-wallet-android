package io.stormbird.wallet.entity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import static io.stormbird.wallet.C.*;

public class HomeReceiver extends BroadcastReceiver
{
    private final HomeCommsInterface homeCommsInterface;
    public HomeReceiver(Activity ctx, HomeCommsInterface homeCommsInterface)
    {
        ctx.registerReceiver(this, new IntentFilter(DOWNLOAD_READY));
        ctx.registerReceiver(this, new IntentFilter(RESET_TOOLBAR));
        ctx.registerReceiver(this, new IntentFilter(REQUEST_NOTIFICATION_ACCESS));
        ctx.registerReceiver(this, new IntentFilter(BACKUP_WALLET_SUCCESS));
        this.homeCommsInterface = homeCommsInterface;
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Bundle bundle = intent.getExtras();
        switch (intent.getAction())
        {
            case DOWNLOAD_READY:
                String message = bundle.getString("Version");
                homeCommsInterface.downloadReady(message);
                break;
            case RESET_TOOLBAR:
                homeCommsInterface.resetToolbar();
                break;
            case REQUEST_NOTIFICATION_ACCESS:
                homeCommsInterface.requestNotificationPermission();
                break;
            case BACKUP_WALLET_SUCCESS:
                String keyAddress = bundle.getString("Key");
                homeCommsInterface.backupSuccess(keyAddress);
                break;
            default:
                break;
        }
    }
}
