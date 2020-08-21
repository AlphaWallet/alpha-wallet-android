package com.alphawallet.app.service;

import android.content.Context;

public class NoAnalyticsService<T> implements AnalyticsServiceType<T>{

    public NoAnalyticsService(Context context)
    {
        //No code
    }

    @Override
    public void track(String eventName)
    {
        //No code
    }

    @Override
    public void track(String eventName, T event)
    {
        //No code
    }

    @Override
    public void identify(String uuid)
    {
        //No code
    }

    @Override
    public void flush()
    {
        //No code
    }
}