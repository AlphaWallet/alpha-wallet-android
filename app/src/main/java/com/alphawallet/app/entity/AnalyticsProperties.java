package com.alphawallet.app.entity;

import org.json.JSONException;
import org.json.JSONObject;

import timber.log.Timber;

public class AnalyticsProperties
{
    private final JSONObject props;

    public AnalyticsProperties()
    {
        props = new JSONObject();
    }

    public void put(String key, Object value)
    {
        try
        {
            props.put(key, value);
        }
        catch (JSONException e)
        {
            Timber.e(e);
        }
    }

    public JSONObject get()
    {
        return props;
    }
}