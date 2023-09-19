package com.alphawallet.app.service;

import android.content.Context;

import com.alphawallet.app.entity.ServiceErrorException;
import com.alphawallet.app.repository.PreferenceRepositoryType;

public class AnalyticsService<T> implements AnalyticsServiceType<T> {

    public AnalyticsService(Context context, PreferenceRepositoryType preferenceRepository)
    {
        //No code
    }

    @Override
    public void increment(String property)
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

    @Override
    public void recordException(ServiceErrorException e)
    {
        //No code
    }
}