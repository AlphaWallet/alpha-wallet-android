package io.stormbird.wallet.entity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import static io.stormbird.wallet.C.PAGE_LOADED;

public class URLLoadReceiver extends BroadcastReceiver
{
    private final URLLoadInterface loadInterface;
    public URLLoadReceiver(Activity ctx, URLLoadInterface loadInterface)
    {
        ctx.registerReceiver(this, new IntentFilter(PAGE_LOADED));
        this.loadInterface = loadInterface;
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Bundle bundle = intent.getExtras();
        switch (intent.getAction())
        {
            case PAGE_LOADED:
                String webUrl = bundle.getString("url");
                String webTitle = bundle.getString("title");
                loadInterface.onWebpageLoaded(webUrl, webTitle);
                break;
        }
    }
}
