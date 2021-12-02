package com.alphawallet.app.util.das;

/**
 * Created by JB on 17/09/2021.
 */
public class DASRecord
{
    String key;
    String label;
    String value;
    String ttl;

    public String getAddress()
    {
        return value;
    }

    public String getLabel()
    {
        return label;
    }
}
