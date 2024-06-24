package com.langitwallet.app.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.langitwallet.app.R;
import com.langitwallet.app.C;
import com.langitwallet.app.entity.Transaction;
import com.langitwallet.app.entity.TransactionType;
import com.langitwallet.app.entity.tokens.Token;
import com.langitwallet.app.entity.transactions.TransferEvent;
import com.langitwallet.app.repository.PreferenceRepositoryType;
import com.langitwallet.app.ui.TransactionDetailActivity;
import com.langitwallet.app.util.BalanceUtils;
import com.langitwallet.app.util.Utils;

import java.util.Locale;

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

    public void showNotification(Transaction tx, Token t, TransferEvent te)
    {
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

        Notification n = createNotification(pendingIntent, tx, t, te);
        if (n != null)
        {
            notificationManager.notify(id, n);
        }
    }

    public Notification createNotification(PendingIntent intent, Transaction tx, Token t, @Nullable TransferEvent te)
    {
        String walletAddress = preferenceRepository.getCurrentWalletAddress();

        boolean defaultCase =
            !preferenceRepository.isWatchOnly() &&
                tx.timeStamp > preferenceRepository.getLoginTime(walletAddress) &&
                preferenceRepository.isTransactionNotificationsEnabled(walletAddress);

        if (te == null)
        {
            TransactionType txType = t.getTransactionType(tx);
            boolean receivedBaseToken =
                (txType.equals(TransactionType.RECEIVED) ||
                    txType.equals(TransactionType.RECEIVE_FROM)) &&
                    tx.to.equalsIgnoreCase(walletAddress);

            if (defaultCase && receivedBaseToken)
            {
                return buildNotification(
                    intent,
                    context.getString(R.string.received),
                    t.getTransactionResultValue(tx),
                    tx.from);
            }
        }
        else
        {
            if (defaultCase && isRecipient(walletAddress, te.valueList))
            {
                boolean isMintEvent = isMintEvent(te.valueList);
                return buildNotification(
                    intent,
                    isMintEvent ?
                        context.getString(R.string.minted) :
                        context.getString(R.string.received),
                    getReadableValue(t, te.tokenValue) + " " + t.getSymbol(),
                    isMintEvent ?
                        "" :
                        tx.from
                );
            }
        }

        return null;
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

    private Notification buildNotification(PendingIntent pendingIntent, String event, String value, String fromAddress)
    {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo)
            .setAutoCancel(true)
            .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000})
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setContentTitle(getTitle(event, value))
            .setContentText(getBody(value, fromAddress))
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText(getBody(value, fromAddress))
            );

        return builder.build();
    }

    private String getTitle(String event, String value)
    {
        return event + " " + value;
    }

    private String getBody(String value, String fromAddress)
    {
        if (TextUtils.isEmpty(fromAddress))
        {
            return context.getString(R.string.notification_message_incoming_token,
                Utils.formatAddress(preferenceRepository.getCurrentWalletAddress()),
                value
            );
        }
        else
        {
            return context.getString(R.string.notification_message_incoming_token_with_recipient,
                Utils.formatAddress(preferenceRepository.getCurrentWalletAddress()),
                value,
                Utils.formatAddress(fromAddress)
            );
        }
    }

    private boolean isMintEvent(String input)
    {
        String searchText = "from,address,";
        int startIndex = input.indexOf(searchText) + searchText.length();
        int endIndex = input.indexOf(",", startIndex);
        String from = input.substring(startIndex, endIndex);
        return from.equalsIgnoreCase(C.BURN_ADDRESS);
    }

    private boolean isRecipient(String walletAddress, String input)
    {
        String validationString = "to,address," + walletAddress.toLowerCase(Locale.ROOT);
        return input.toLowerCase(Locale.ROOT).contains(validationString);
    }

    private String getReadableValue(Token t, String tokenValue)
    {
        if (t.isERC20())
        {
            return BalanceUtils.getScaledValue(tokenValue, t.tokenInfo.decimals);
        }
        else
        {
            return "#" + tokenValue;
        }
    }
}
