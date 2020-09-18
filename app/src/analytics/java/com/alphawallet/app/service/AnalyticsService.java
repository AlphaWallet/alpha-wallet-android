package com.alphawallet.app.service;

import android.content.Context;
import android.os.Bundle;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.AnalyticsProperties;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.iid.FirebaseInstanceId;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class AnalyticsService<T> implements AnalyticsServiceType<T> {

    private MixpanelAPI mixpanelAPI;
    private FirebaseAnalytics firebaseAnalytics;

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
        if(!analyticsProperties.getWalletType().isEmpty())
        {
            props.putString(C.AN_WALLET_TYPE, analyticsProperties.getWalletType());
        }

        firebaseAnalytics.logEvent(eventName, props);
    }

    private void trackMixpanel(AnalyticsProperties analyticsProperties, String eventName)
    {
        try
        {
            JSONObject props = new JSONObject();

            if(analyticsProperties.getWalletType() != null)
            {
                props.put(C.AN_WALLET_TYPE, analyticsProperties.getWalletType());
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
}