package com.alphawallet.app.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import com.alphawallet.app.R;
import com.alphawallet.app.interact.FetchWalletsInteract;
import com.alphawallet.app.repository.TransactionRepository;
import com.alphawallet.app.repository.TransactionRepositoryType;
import com.alphawallet.app.ui.WalletConnectSessionActivity;
import com.walletconnect.walletconnectv2.client.WalletConnectClient;

import javax.inject.Inject;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class WalletConnectV2Service extends Service
{
    @Inject
    AWWalletConnectClient awWalletConnectClient;

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate()
    {
        super.onCreate();
        String CHANNEL_ID = "my_channel_01";
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "WalletConnect V2",
                NotificationManager.IMPORTANCE_DEFAULT);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);

        Intent intent = new Intent(getApplicationContext(), WalletConnectSessionActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle("WalletConnect is active")
                .setContentText("Click to view active sessions")
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);
        notificationManager.notify(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        WalletConnectClient.INSTANCE.setWalletDelegate(awWalletConnectClient);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        stopForeground(true);
    }
}
