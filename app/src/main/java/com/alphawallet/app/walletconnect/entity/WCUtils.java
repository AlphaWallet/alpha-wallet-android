package com.alphawallet.app.walletconnect.entity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletConnectActions;
import com.alphawallet.app.repository.entity.RealmWCSession;
import com.alphawallet.app.service.WalletConnectService;
import com.alphawallet.app.walletconnect.WCClient;
import com.alphawallet.app.walletconnect.WCSession;

import java.util.ArrayList;
import java.util.Collections;

public abstract class WCUtils
{
    public static void startServiceLocal(Context context, ServiceConnection connection, WalletConnectActions action)
    {
        Intent i = new Intent(context, WalletConnectService.class);
        i.setAction(String.valueOf(action.ordinal()));
        ActivityManager.RunningAppProcessInfo myProcess = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(myProcess);
        boolean isInBackground = myProcess.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
        if (!isInBackground)
        {
            context.startService(i);
            if (connection != null) context.bindService(i, connection, Context.BIND_ABOVE_CLIENT);
        }
    }

    public static WCClient createWalletConnectSession(Activity activity, Wallet wallet, WCSession session, String peerId, String remotePeerId)
    {
        WCClient client = new WCClient();

        WCPeerMeta peerMeta = new WCPeerMeta(
                activity.getString(R.string.app_name),
                C.ALPHAWALLET_WEB,
                wallet.address,
                new ArrayList<>(Collections.singleton(C.ALPHAWALLET_LOGO_URI))
        );

        client.connect(session, peerMeta, peerId, remotePeerId);

        return client;
    }
}
