package com.alphawallet.app.service;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.entity.AnalyticsProperties;
import com.alphawallet.app.entity.ServiceErrorException;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.iid.FirebaseInstanceId;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class AnalyticsService<T> implements AnalyticsServiceType<T> {

    private final MixpanelAPI mixpanelAPI;
    private final FirebaseAnalytics firebaseAnalytics;

    public static native String getAnalyticsKey();

    static {
        System.loadLibrary("keys");
    }

    public AnalyticsService(Context context)
    {
        mixpanelAPI = MixpanelAPI.getInstance(context, getAnalyticsKey());
        firebaseAnalytics = FirebaseAnalytics.getInstance(context);
    }

    @Override
    public void track(String eventName)
    {
        //firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, eventName);
        mixpanelAPI.track(eventName);
    }

    @Override
    public void track(String eventName, T event)
    {
        AnalyticsProperties analyticsProperties = (AnalyticsProperties) event;

        trackFirebase(analyticsProperties, eventName);
        trackMixpanel(analyticsProperties, eventName);
    }

    private void trackFirebase(AnalyticsProperties analyticsProperties, String eventName)
    {
        Bundle props = new Bundle();
        if(!TextUtils.isEmpty(analyticsProperties.getWalletType()))
        {
            props.putString(C.AN_WALLET_TYPE, analyticsProperties.getWalletType());
        }

        if(!TextUtils.isEmpty(analyticsProperties.getData()))
        {
            props.putString(C.AN_USE_GAS, analyticsProperties.getData());
        }

        props.putString(C.APP_NAME, BuildConfig.APPLICATION_ID);

        firebaseAnalytics.logEvent(eventName, props);
    }

    private void trackMixpanel(AnalyticsProperties analyticsProperties, String eventName)
    {
        try
        {
            JSONObject props = new JSONObject();

            if (!TextUtils.isEmpty(analyticsProperties.getWalletType()))
            {
                props.put(C.AN_WALLET_TYPE, analyticsProperties.getWalletType());
            }

            if (!TextUtils.isEmpty(analyticsProperties.getData()))
            {
                props.put(C.AN_USE_GAS, analyticsProperties.getData());
            }

            mixpanelAPI.track(eventName, props);
        }
        catch(JSONException e)
        {
            //Something went wrong
        }
    }

    @Override
    public void identify(String uuid)
    {
        firebaseAnalytics.setUserId(uuid);
        mixpanelAPI.identify(uuid);
        mixpanelAPI.getPeople().identify(uuid);
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful())
                    {
                        String token = Objects.requireNonNull(task.getResult()).getToken();
                        mixpanelAPI.getPeople().setPushRegistrationId(token);
                    }
                });
    }

    @Override
    public void flush()
    {
        //Nothing like flush in firebase
        mixpanelAPI.flush();
    }

    @Override
    public void recordException(ServiceErrorException e)
    {
        FirebaseCrashlytics.getInstance().recordException(e);
    }
}