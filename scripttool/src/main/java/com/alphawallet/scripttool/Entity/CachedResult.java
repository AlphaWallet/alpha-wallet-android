package com.alphawallet.scripttool.Entity;

public class CachedResult
{
    public long resultTime;
    public String result;

    public CachedResult(long time, String r)
    {
        resultTime = time;
        result = r;
    }
}
