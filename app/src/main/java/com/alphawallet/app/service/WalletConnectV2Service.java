package com.alphawallet.app.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.WalletConnectNotificationActivity;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class WalletConnectV2Service extends Service
{
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

        Intent intent = new Intent(getApplicationContext(), WalletConnectNotificationActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_logo)
                .setContentTitle(getString(R.string.notify_wallet_connect_title))
                .setContentText(getString(R.string.notify_wallet_connect_content))
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);
        notificationManager.notify(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        stopForeground(true);
    }
}
