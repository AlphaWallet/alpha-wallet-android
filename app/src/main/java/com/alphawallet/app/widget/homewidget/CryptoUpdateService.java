package com.alphawallet.app.widget.homewidget;

import static com.alphawallet.app.widget.homewidget.CryptoUpdateService.LOCATION.FINAL;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.alphawallet.app.R;
import com.alphawallet.app.util.Utils;

import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class CryptoUpdateService extends Service
{
    private Disposable disposable;

    public enum LOCATION
    {
        UPDATE,
        GOT_HOME,
        ACTION_TOGGLE,
        RESTART,
        CRYPTOS_READY,
        RE_CONNECTED,

        FINAL
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        try
        {
            int cValue = Integer.parseInt(intent.getAction());

            if (cValue < FINAL.ordinal())
            {
                LOCATION l = LOCATION.values()[cValue];

                switch (l)
                {
                    case RE_CONNECTED:
                        pingAPI();
                        break;

                    case UPDATE:
                        pingAPI();
                        break;

                    case ACTION_TOGGLE:
                        break;

                    case RESTART:
                        pingAPI();
                        break;

                    case CRYPTOS_READY:
                        updateWidgets(null);
                        break;

                    default:
                        pingAPI();
                        break;
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return START_STICKY;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        Utils.scheduleJob(getApplicationContext()); //ensure we always start
        if (Build.VERSION.SDK_INT >= 26)
        {
            String CHANNEL_ID = "my_channel_01";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_NONE);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("")
                    .setContentText("")
                    .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                    .setPriority(NotificationCompat.PRIORITY_MIN).build();

            startForeground(1, notification);
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (disposable != null)
        {
            disposable.dispose();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    private void pingAPI()
    {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        CoinList.initCoins(this);

        AppWidgetManager man = AppWidgetManager.getInstance(this);
        int[] ids = man.getAppWidgetIds(new ComponentName(this, HomeScreenTickerProvider.class));

        String fiatSelected = sp.getString("FIATSTR" , "USD"); //Default to USD

        CoinData[] data = CoinList.getCoinData();

        if (ids.length > 0)
        {
            disposable = new WebQuery().updateCryptoTickers(fiatSelected, data)
                    .map(coinData -> CoinList.populateCryptoList(this, coinData))
                    .subscribeOn(Schedulers.io())
                    .subscribe(this::updateWidgets, this::onAPIError);
        }

        Utils.scheduleJob(this);
    }

    private void onAPIError(Throwable error)
    {
        //still run the update widget
        updateWidgets(null);
    }

    private void updateWidgets(CoinData[] data)
    {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (data == null)
        {
            CoinList.initCoins(this);
            data = CoinList.getCoinData();
        }

        AppWidgetManager man = AppWidgetManager.getInstance(this);
        int[] ids = man.getAppWidgetIds(new ComponentName(this, HomeScreenTickerProvider.class));

        for (int widgetId : ids)
        {
            //pull data for this widget
            String key = "pre" + widgetId;
            String cryptoStr = sp.getString(key + "CRYPTSTR", "BTC");
            String fiatStr  = sp.getString("FIATSTR", "USD");

            float currentCrypto = CoinList.getCryptoValue(cryptoStr);
            if (currentCrypto >= 0 )
            {
                float crypto1h = CoinList.getCrypto1hChange(cryptoStr);
                float crypto24h = CoinList.getCrypto24hChange(cryptoStr);
                float crypto7d = CoinList.getCrypto7dChange(cryptoStr);
                String cryptoName = CoinList.getCryptoName(cryptoStr);

                //Restart app if widget clicked outside of the box, keeping track of which widget was clicked
                Intent resultIntent = new Intent(this, HomeScreenTickerConfigureActivity.class);
                resultIntent.setAction("startWidget" + widgetId);
                resultIntent.putExtra("id", widgetId);
                resultIntent.setFlags(widgetId);
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                stackBuilder.addParentStack(HomeScreenTickerConfigureActivity.class);
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent rPI = stackBuilder.getPendingIntent(key.hashCode(), PendingIntent.FLAG_UPDATE_CURRENT);

                RemoteViews remoteViews = new RemoteViews(getApplicationContext().getPackageName(),
                        R.layout.layout_widget_crypto);
                // Set the text
                String truncValue = String.format(java.util.Locale.US, "%.2f", currentCrypto);

                if (currentCrypto < 1 && currentCrypto > 0)
                {
                    truncValue = String.format(java.util.Locale.US, "%.4f", currentCrypto);
                }

                remoteViews.setTextViewText(R.id.textCrypto, cryptoName);
                remoteViews.setTextViewText(R.id.textValue, truncValue);
                remoteViews.setTextViewText(R.id.textType, fiatStr);
                updateCallAge(remoteViews);

                setChange(remoteViews, crypto1h, R.id.textChangeGreen, R.id.textChangeRed);
                setChange(remoteViews, crypto24h, R.id.text24hChangeGreen, R.id.text24hChangeRed);
                setChange(remoteViews, crypto7d, R.id.text7dChangeGreen, R.id.text7dChangeRed);

                remoteViews.setOnClickPendingIntent(R.id.relLayout, rPI);

                man.updateAppWidget(widgetId, remoteViews);
            }
        }
    }

    private void setChange(RemoteViews remoteViews, float change, int positiveId, int negativeId)
    {
        if (change >= 0)
        {
            String truncChange = String.format(java.util.Locale.US, "+%.2f", change);
            remoteViews.setTextViewText(positiveId, truncChange);
            remoteViews.setTextViewText(negativeId, truncChange);
            remoteViews.setViewVisibility(negativeId, View.INVISIBLE);
            remoteViews.setViewVisibility(positiveId, View.VISIBLE);
        }
        else
        {
            String truncChange = String.format(java.util.Locale.US, "%.2f", change);
            remoteViews.setTextViewText(negativeId, truncChange);
            remoteViews.setTextViewText(positiveId, truncChange);
            remoteViews.setViewVisibility(positiveId, View.INVISIBLE);
            remoteViews.setViewVisibility(negativeId, View.VISIBLE);
        }
    }

    private void updateCallAge(RemoteViews remoteViews)
    {
        long timeDiff = CoinList.getLastAPIReturnDiff(this);

        if (timeDiff < 10*60)
        {
            remoteViews.setViewVisibility(R.id.textCounterGreen, View.VISIBLE);
            remoteViews.setViewVisibility(R.id.textCounterRed, View.GONE);
        }
        else
        {
            remoteViews.setViewVisibility(R.id.textCounterGreen, View.GONE);
            remoteViews.setViewVisibility(R.id.textCounterRed, View.VISIBLE);
            String diffUnit = "minutes";
            if (timeDiff > 60 * 60 * 24)
            {
                diffUnit = "days";

                timeDiff = timeDiff / (60 * 24);
            }
            else if (timeDiff > 60 * 60 * 2)
            {
                diffUnit = "hours";
                timeDiff = timeDiff / 60;
            }

            remoteViews.setTextViewText(R.id.textCounterRed, String.valueOf(timeDiff / 60) + " " + diffUnit);
        }
    }
}
