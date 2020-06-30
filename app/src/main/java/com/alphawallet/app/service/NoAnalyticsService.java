package com.alphawallet.app.service;

import android.content.Context;

public class NoAnalyticsService<T> implements AnalyticsServiceType<T>{

    public NoAnalyticsService(Context context)
    {
    }

    @Override
    public void track(String eventName)
    {

    }

    @Override
    public void track(String eventName, T event)
    {

    }

    @Override
    public void identify(String uuid)
    {

    }

    @Override
    public void flush(){

    }
}
