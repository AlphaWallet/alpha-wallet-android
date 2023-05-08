package com.alphawallet.app.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.Transaction;
import com.alphawallet.app.entity.TransactionType;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.ui.TransactionDetailActivity;
import com.alphawallet.app.util.Utils;

public class TransactionNotificationService
{
    private static final String CHANNEL_ID = "transactions_channel";
    private static final String CHANNEL_NAME = "Transactions";
    private final Context context;
    private final PreferenceRepositoryType preferenceRepository;

    public TransactionNotificationService(
        Context context,
        PreferenceRepositoryType preferenceRepositoryType
    )
    {
        this.context = context;
        this.preferenceRepository = preferenceRepositoryType;
    }

    public void showNotification(Transaction tx, Token t)
    {
        if (!shouldShowNotification(tx, t))
        {
            return;
        }

        int id = tx.hash != null ? tx.hash.hashCode() : 0;

        PendingIntent pendingIntent
            = PendingIntent.getActivity(
            context, id, buildIntent(tx, t),
            PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationManager notificationManager =
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) // Oreo and above requires notification channels
        {
            NotificationChannel notificationChannel =
                new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                );

            notificationManager.createNotificationChannel(notificationChannel);
        }

        notificationManager.notify(id, buildNotification(pendingIntent, tx, t));
    }

    public boolean shouldShowNotification(Transaction tx, Token t)
    {
        String walletAddress = preferenceRepository.getCurrentWalletAddress();
        TransactionType txType = t.getTransactionType(tx);

        return (txType.equals(TransactionType.RECEIVED) ||
            txType.equals(TransactionType.RECEIVE_FROM)) &&
            !preferenceRepository.isWatchOnly() &&
            preferenceRepository.isTransactionNotificationsEnabled(walletAddress) &&
            tx.to.equalsIgnoreCase(walletAddress) &&
            tx.timeStamp > preferenceRepository.getWalletCreationTime();
    }

    private Intent buildIntent(Transaction tx, Token t)
    {
        Intent intent = new Intent(context, TransactionDetailActivity.class);
        intent.putExtra(C.EXTRA_TXHASH, tx.hash);
        intent.putExtra(C.EXTRA_CHAIN_ID, t.tokenInfo.chainId);
        intent.putExtra(C.EXTRA_ADDRESS, t.getAddress());
        intent.putExtra(C.FROM_NOTIFICATION, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        return intent;
    }

    private Notification buildNotification(PendingIntent pendingIntent, Transaction tx, Token t)
    {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo)
            .setAutoCancel(true)
            .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000})
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setContentTitle(getTitle(tx, t))
            .setContentText(getBody(tx, t))
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText(getBody(tx, t))
            );

        return builder.build();
    }

    private String getTitle(Transaction tx, Token t)
    {
        return t.getOperationName(tx, context) + " " + t.getTransactionResultValue(tx);
    }

    private String getBody(Transaction tx, Token t)
    {
        return context.getString(R.string.notification_message_incoming_token,
            Utils.formatAddress(preferenceRepository.getCurrentWalletAddress()),
            t.getTransactionResultValue(tx),
            Utils.formatAddress(tx.from)
        );
    }
}
