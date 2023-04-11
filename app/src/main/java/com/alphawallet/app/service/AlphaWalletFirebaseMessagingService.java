package com.alphawallet.app.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.notification.DataMessage;
import com.alphawallet.app.ui.HomeActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public class AlphaWalletFirebaseMessagingService extends FirebaseMessagingService
{
    @Inject
    TokensService tokensService;

    /**
     * There are two scenarios when onNewToken is called:
     * 1) When a new token is generated on initial app startup
     * 2) Whenever an existing token is changed
     * Under #2, there are three scenarios when the existing token is changed:
     * A) App is restored to a new device
     * B) User uninstalls/reinstalls the app
     * C) User clears app data
     */
    @Override
    public void onNewToken(@NonNull String token)
    {
        // Timber.d("token: " + token);
        // sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token)
    {
        // TODO:
        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage)
    {
        super.onMessageReceived(remoteMessage);
        DataMessage.Title title = new Gson().fromJson(remoteMessage.getData().get("title"), DataMessage.Title.class);
        DataMessage.Body body = new Gson().fromJson(remoteMessage.getData().get("body"), DataMessage.Body.class);
        Timber.d("data = " + remoteMessage.getData());

        // TODO: Initiate token fetch here
    }

    public void showNotification(RemoteMessage remoteMessage)
    {
        // Pass the intent to switch to the MainActivity
        Intent intent = new Intent(this, HomeActivity.class);
        // Assign channel ID
        String channel_id = "notification_channel";
        // Here FLAG_ACTIVITY_CLEAR_TOP flag is set to clear
        // the activities present in the activity stack,
        // on the top of the Activity that is to be launched

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setAction(C.NOTIFICATION_RECEIVED);
        Bundle extras = new Bundle();
        extras.putString("data", remoteMessage.getData().toString());
        intent.putExtras(extras);

        // Pass the intent to PendingIntent to start the
        // next Activity
        PendingIntent pendingIntent
            = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT);

        // Create a Builder object using NotificationCompat
        // class. This will allow control over all the flags
        NotificationCompat.Builder builder
            = new NotificationCompat
            .Builder(getApplicationContext(),
            channel_id)
            .setSmallIcon(R.drawable.ic_logo)
            .setAutoCancel(true)
            .setVibrate(new long[]{1000, 1000, 1000,
                1000, 1000})
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent);

        builder = builder.setContentTitle("Title")
            .setContentText("Message")
            .setSmallIcon(R.drawable.ic_logo);

        // Create an object of NotificationManager class to
        // notify the user of events that happen in the background.
        NotificationManager notificationManager
            = (NotificationManager) getSystemService(
            Context.NOTIFICATION_SERVICE);
        // Check if the Android Version is greater than Oreo
        if (Build.VERSION.SDK_INT
            >= Build.VERSION_CODES.O)
        {
            NotificationChannel notificationChannel
                = new NotificationChannel(
                channel_id, "web_app",
                NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(
                notificationChannel);
        }

        notificationManager.notify(0, builder.build());
    }
}
