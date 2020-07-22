package com.alphawallet.app.service;

import android.content.Context;

public class NoAnalyticsService<T> implements AnalyticsServiceType<T>{

    public NoAnalyticsService(Context context)
    {
        //No Code
    }

    @Override
    public void track(String eventName)
    {
        //No Code
    }

    @Override
    public void track(String eventName, T event)
    {
        //No Code
    }

    @Override
    public void identify(String uuid)
    {
        //No Code
    }

    @Override
    public void flush()
    {
        //No Code
    }
}