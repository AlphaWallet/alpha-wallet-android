package com.alphawallet.app.service;

import android.content.Context;
import android.os.Bundle;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.AnalyticsProperties;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

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
    }

    @Override
    public void track(String eventName, T event)
    {
        AnalyticsProperties analyticsProperties = (AnalyticsProperties) event;

        Bundle props = new Bundle();
        if (!analyticsProperties.getWalletType().isEmpty())
        {
            props.putString(C.AN_WALLET_TYPE, analyticsProperties.getWalletType());
        }

        firebaseAnalytics.logEvent(eventName, props);
    }

    @Override
    public void identify(String uuid)
    {
        firebaseAnalytics.setUserId(uuid);
    }

    @Override
    public void flush()
    {
        //Nothing like flush in firebase
    }
}