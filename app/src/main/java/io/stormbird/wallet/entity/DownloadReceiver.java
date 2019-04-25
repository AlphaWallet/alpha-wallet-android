package io.stormbird.wallet.entity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import static io.stormbird.wallet.C.*;

public class DownloadReceiver extends BroadcastReceiver
{
    private final DownloadInterface downloadInterface;
    public DownloadReceiver(Activity ctx, DownloadInterface downloadInterface)
    {
        ctx.registerReceiver(this, new IntentFilter(DOWNLOAD_READY));
        ctx.registerReceiver(this, new IntentFilter(RESET_TOOLBAR));
        ctx.registerReceiver(this, new IntentFilter(REQUEST_NOTIFICATION_ACCESS));
        this.downloadInterface = downloadInterface;
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Bundle bundle = intent.getExtras();
        switch (intent.getAction())
        {
            case DOWNLOAD_READY:
                String message = bundle.getString("Version");
                downloadInterface.downloadReady(message);
                break;
            case RESET_TOOLBAR:
                downloadInterface.resetToolbar();
                break;
            case REQUEST_NOTIFICATION_ACCESS:
                downloadInterface.requestNotificationPermission();
                break;
            default:
                break;
        }
    }
}
