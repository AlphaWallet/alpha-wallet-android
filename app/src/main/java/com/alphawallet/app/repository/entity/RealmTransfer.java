package com.alphawallet.app.repository.entity;

import io.realm.RealmObject;

/**
 * Created by JB on 17/12/2020.
 */
public class RealmTransfer implements RealmObject
{
    private String hash;
    private String tokenAddress;
    private String eventName;
    private String transferDetail;

    public String getHash()
    {
        return hash;
    }
    public void setHash(String hash) { this.hash = hash; }

    public String getTokenAddress()
    {
        return tokenAddress;
    }

    public void setTokenAddress(String address)
    {
        tokenAddress = address;
    }

    public void setTransferDetail(String transferDetail)
    {
        this.transferDetail = transferDetail;
    }

    public String getTransferDetail()
    {
        return transferDetail;
    }

    public String getEventName()
    {
        return eventName;
    }

    public void setEventName(String eventName)
    {
        this.eventName = eventName;
    }
}
