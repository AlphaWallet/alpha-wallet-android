package com.alphawallet.app.repository;

/**
 * Created by JB on 17/12/2020.
 */
public class EventResult
{
    public final String type;
    public final String value;

    public EventResult(String t, String v)
    {
        type = t;
        value = v;
    }
}
