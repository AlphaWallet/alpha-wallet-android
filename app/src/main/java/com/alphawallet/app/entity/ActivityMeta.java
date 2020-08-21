package com.alphawallet.app.entity;

/**
 * Created by JB on 7/07/2020.
 */
public class ActivityMeta
{
    public final long timeStamp;
    public final String hash;

    public ActivityMeta(long ts, String txHash)
    {
        timeStamp = ts;
        hash = txHash;
    }
}
