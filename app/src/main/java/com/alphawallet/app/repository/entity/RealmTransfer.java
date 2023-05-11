package com.alphawallet.app.repository.entity;

import io.realm.RealmObject;

/**
 * Created by JB on 17/12/2020.
 */
public class RealmTransfer extends RealmObject
{
    private String hash;
    private String tokenAddress;
    private String eventName;
    private String transferDetail;

    public String getHash()
    {
        String[] split = hash.split("-");
        if (split.length > 0)
        {
            return split[0];
        }
        else
        {
            return "";
        }
    }
    public void setHashKey(long chainId, String hash)
    {
        this.hash = databaseKey(chainId, hash);
    }

    public long getChain()
    {
        String[] split = hash.split("-");
        if (split.length > 1)
        {
            return Long.parseLong(split[1]);
        }
        else
        {
            return 0L;
        }
    }

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

    public static String databaseKey(long chainId, String hash)
    {
        return hash + "-" + chainId;
    }
}
