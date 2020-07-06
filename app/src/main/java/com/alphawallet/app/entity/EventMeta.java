package com.alphawallet.app.entity;

import com.alphawallet.token.entity.ContractAddress;

/**
 * Created by JB on 7/07/2020.
 */
public class EventMeta extends ActivityMeta
{
    public final String eventMessage;
    public final ContractAddress tokenAddress;

    public EventMeta(String message, long timeStamp, int chainId, String address)
    {
        super(timeStamp);
        eventMessage = message;
        tokenAddress = new ContractAddress(chainId, address);
    }
}
