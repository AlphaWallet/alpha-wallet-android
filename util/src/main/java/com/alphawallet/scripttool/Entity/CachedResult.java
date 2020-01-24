package com.alphawallet.scripttool.Entity;

public class CachedResult
{
    public final long resultTime;
    public final String result;

    public CachedResult(long time, String r)
    {
        resultTime = time;
        result = r;
    }
}
