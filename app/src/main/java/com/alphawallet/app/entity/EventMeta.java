package com.alphawallet.app.entity;

/**
 * Created by JB on 3/04/2020.
 */
public class EventMeta
{
    public final String hash;
    public final long timeStamp;
    public final String eventDisplay;
    public final int chainId;

    public EventMeta(String hash, long timeStamp, String message, int chainId)
    {
        this.hash = hash;
        this.timeStamp = timeStamp;
        this.eventDisplay = message;
        this.chainId = chainId;
    }
}
