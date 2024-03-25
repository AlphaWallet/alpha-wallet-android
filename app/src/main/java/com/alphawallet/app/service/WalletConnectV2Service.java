package com.alphawallet.app.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
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
    private static final String TAG = WalletConnectV2Service.class.getName();

    final String CHANNEL_ID = "WalletConnectV2Service";
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
    }

    private Notification createNotification()
    {
        Intent notificationIntent = new Intent(this, WalletConnectNotificationActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notify_wallet_connect_title))
                .setContentText(getString(R.string.notify_wallet_connect_content))
                .setSmallIcon(R.drawable.ic_logo)
                .setContentIntent(pendingIntent)
                .build();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel()
    {
        CharSequence name = getString(R.string.notify_wallet_connect_title);
        String description = getString(R.string.notify_wallet_connect_content);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(description);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = createNotification();
        startForeground(1, notification);
        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        stopForeground(true);
    }
}
