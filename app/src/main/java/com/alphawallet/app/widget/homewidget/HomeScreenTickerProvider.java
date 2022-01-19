package com.alphawallet.app.widget.homewidget;

import static com.alphawallet.app.widget.homewidget.CryptoUpdateService.LOCATION.ACTION_TOGGLE;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.alphawallet.app.R;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link HomeScreenTickerConfigureActivity HomeScreenTickerConfigureActivity}
 */
public class HomeScreenTickerProvider extends AppWidgetProvider {

    //Wizard added function
//    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
//                                int appWidgetId) {
//
//        CharSequence widgetText = HomeScreenTickerConfigureActivity.loadTitlePref(context, appWidgetId);
//        // Construct the RemoteViews object
//        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.layout_widget_crypto);
//        views.setTextViewText(R.id.appwidget_text, widgetText);
//
//        // Instruct the widget manager to update the widget
//        appWidgetManager.updateAppWidget(appWidgetId, views);
//    }

    @SuppressLint("NewApi")
    private RemoteViews inflateLayout(Context context, int appWidgetId)
    {
        Intent resultIntent = new Intent(context, HomeScreenTickerConfigureActivity.class);
        resultIntent.setAction("startWidget" + appWidgetId);
        resultIntent.putExtra("id", appWidgetId);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(HomeScreenTickerConfigureActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent rPI = stackBuilder.getPendingIntent(0,  PendingIntent.FLAG_UPDATE_CURRENT);

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.layout_widget_crypto);
        remoteViews.setOnClickPendingIntent(R.id.relLayout, rPI);

        //Call the service
        Intent service = new Intent(context, CryptoUpdateService.class);
        service.setAction(String.valueOf(CryptoUpdateService.LOCATION.UPDATE.ordinal()));
        context.startService(service);

        return remoteViews;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager man, int[] appWidgetIds)
    {
        try
        {
            RemoteViews remoteView;

            //we only refresh the widget here if there's not an active service
            for (int widgetId : appWidgetIds)
            {
                remoteView = getRemoteViewFromState(context, widgetId);

                if (remoteView != null)
                {
                    setRemoteView(man, context, widgetId, remoteView);
                }
            }
        }
        catch (Exception e)
        {

        }

        super.onUpdate(context, man, appWidgetIds);
    }

    @SuppressLint("NewApi")
    private void setRemoteView(AppWidgetManager appWidgetManager, Context context,
                               int appWidgetId, RemoteViews remoteView)
    {
        Intent resultIntent = new Intent(context, HomeScreenTickerConfigureActivity.class);
        resultIntent.setAction("startWidget" + appWidgetId);
        resultIntent.putExtra("id", appWidgetId);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(HomeScreenTickerConfigureActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent rPI = stackBuilder.getPendingIntent(0,  PendingIntent.FLAG_UPDATE_CURRENT);
        remoteView.setOnClickPendingIntent(R.id.relLayout, rPI);
        appWidgetManager.updateAppWidget(appWidgetId, remoteView);
    }

    private RemoteViews getRemoteViewFromState(Context context, int widgetId)
    {
        RemoteViews remoteView = inflateLayout(context, widgetId);
        Intent startIntent = new Intent(context, CryptoUpdateService.class);
        startIntent.setAction(String.valueOf(ACTION_TOGGLE.ordinal()));
        return remoteView;
    }
}