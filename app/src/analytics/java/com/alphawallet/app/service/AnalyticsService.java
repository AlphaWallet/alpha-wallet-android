package com.alphawallet.app.service;

import android.content.Context;
import android.os.Bundle;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.entity.AnalyticsProperties;
import com.alphawallet.app.entity.ServiceErrorException;
import com.alphawallet.app.repository.KeyProviderFactory;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.iid.FirebaseInstanceId;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Objects;

import timber.log.Timber;

public class AnalyticsService<T> implements AnalyticsServiceType<T>
{

    private final MixpanelAPI mixpanelAPI;
    private final FirebaseAnalytics firebaseAnalytics;

    public AnalyticsService(Context context)
    {
        mixpanelAPI = MixpanelAPI.getInstance(context, KeyProviderFactory.get().getAnalyticsKey());
        firebaseAnalytics = FirebaseAnalytics.getInstance(context);
    }

    public static Bundle jsonToBundle(JSONObject jsonObject) throws JSONException
    {
        Bundle bundle = new Bundle();
        Iterator iter = jsonObject.keys();
        while (iter.hasNext())
        {
            String key = (String) iter.next();
            String value = jsonObject.getString(key);
            bundle.putString(key, value);
        }
        return bundle;
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
        Bundle props;
        try
        {
            props = jsonToBundle(analyticsProperties.get());
            props.putString(C.APP_NAME, BuildConfig.APPLICATION_ID);
            firebaseAnalytics.logEvent(eventName, props);
        }
        catch (JSONException e)
        {
            Timber.e(e);
        }
    }

    private void trackMixpanel(AnalyticsProperties analyticsProperties, String eventName)
    {
        mixpanelAPI.track(eventName, analyticsProperties.get());
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