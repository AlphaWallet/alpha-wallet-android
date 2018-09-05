package io.stormbird.wallet.entity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import static io.stormbird.wallet.C.DOWNLOAD_READY;
import static io.stormbird.wallet.C.RESET_TOOLBAR;

public class DownloadReceiver extends BroadcastReceiver
{
    private final DownloadInterface downloadInterface;
    public DownloadReceiver(Activity ctx, DownloadInterface downloadInterface)
    {
        ctx.registerReceiver(this, new IntentFilter(DOWNLOAD_READY));
        ctx.registerReceiver(this, new IntentFilter(RESET_TOOLBAR));
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
            default:
                break;
        }
    }
}
