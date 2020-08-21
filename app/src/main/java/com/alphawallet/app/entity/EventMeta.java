package com.alphawallet.app.entity;

import java.util.UUID;

/**
 * Created by JB on 7/07/2020.
 */
public class EventMeta extends ActivityMeta
{
    public final int chainId;
    public final String eventName;
    public final String activityCardName;

    public EventMeta(String txHash, String eName, String cardName, long timeStamp, int chain)
    {
        super(timeStamp, txHash);
        chainId = chain;
        eventName = eName;
        activityCardName = cardName;
    }

    public long getUID()
    {
        return UUID.nameUUIDFromBytes((this.hash + eventName).getBytes()).getMostSignificantBits();
    }
}
