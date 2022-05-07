package com.alphawallet.app.repository;

import android.text.TextUtils;

/**
 * Created by JB on 17/12/2020.
 */
public class EventResult
{
    public final String type;
    public final String value;
    public final String[] values;

    public EventResult(String t, String v)
    {
        type = t;

        if (!TextUtils.isEmpty(v))
        {
            values = v.split("-");
            value = values[0];
        }
        else
        {
            values = new String[0];
            value = "";
        }
    }
}
