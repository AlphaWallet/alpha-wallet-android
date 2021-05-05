package com.alphawallet.app.entity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.alphawallet.app.C;

import static com.alphawallet.app.C.ADDED_TOKEN;

public class HomeReceiver extends BroadcastReceiver
{
    private final HomeCommsInterface homeCommsInterface;
    public HomeReceiver(Activity ctx, HomeCommsInterface homeCommsInterface)
    {
        ctx.registerReceiver(this, new IntentFilter(C.DOWNLOAD_READY));
        ctx.registerReceiver(this, new IntentFilter(C.RESET_TOOLBAR));
        ctx.registerReceiver(this, new IntentFilter(C.REQUEST_NOTIFICATION_ACCESS));
        ctx.registerReceiver(this, new IntentFilter(C.BACKUP_WALLET_SUCCESS));
        ctx.registerReceiver(this, new IntentFilter(C.CHANGE_CURRENCY));
        ctx.registerReceiver(this, new IntentFilter(C.RESET_WALLET));
        ctx.registerReceiver(this, new IntentFilter(C.ADDED_TOKEN));
        ctx.registerReceiver(this, new IntentFilter(C.CHANGED_LOCALE));
        ctx.registerReceiver(this, new IntentFilter(C.RESET_TRANSACTIONS));
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
            case C.RESET_TOOLBAR:
                homeCommsInterface.resetToolbar();
                break;
            case C.REQUEST_NOTIFICATION_ACCESS:
                homeCommsInterface.requestNotificationPermission();
                break;
            case C.BACKUP_WALLET_SUCCESS:
                String keyAddress = bundle.getString("Key");
                homeCommsInterface.backupSuccess(keyAddress);
                break;
            case C.CHANGE_CURRENCY:
                homeCommsInterface.changeCurrency();
                break;
            case C.RESET_WALLET:
                homeCommsInterface.resetTokens();
                break;
            case ADDED_TOKEN:
                homeCommsInterface.addedToken(intent.getParcelableArrayListExtra(C.EXTRA_TOKENID_LIST));
                break;
            case C.CHANGED_LOCALE:
                homeCommsInterface.changedLocale();
                break;
            case C.RESET_TRANSACTIONS:
                homeCommsInterface.resetTransactions();
                break;
            case C.WALLET_CONNECT_REQUEST:
                String sessionId = bundle.getString("sessionid");
                homeCommsInterface.openWalletConnect(sessionId);
            default:
                break;
        }
    }
}
