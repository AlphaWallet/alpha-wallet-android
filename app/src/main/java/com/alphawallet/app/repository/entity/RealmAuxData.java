package com.alphawallet.app.repository.entity;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

import java.math.BigInteger;

/**
 * Created by James on 6/05/2019.
 * Stormbird in Sydney
 */
public class RealmAuxData extends RealmObject
{
    @PrimaryKey
    private String instanceKey; //should be token address, token Id, chainId
    private int chainId;
    private String tokenAddress;
    private String tokenId;
    private String functionId;
    private String result;
    private long resultTime;

    public String getInstanceKey()
    {
        return instanceKey;
    }

    public int getChainId()
    {
        return chainId;
    }

    public String getTransactionHash()
    {
        String[] split = instanceKey.split("-");
        if (split.length > 0) return split[0];
        else return "";
    }

    public String getEventName()
    {
        String[] split = instanceKey.split("-");
        if (split.length > 1) return split[1];
        else return "";
    }

    public void setChainId(int chainId)
    {
        this.chainId = chainId;
    }

    public BigInteger getTokenId()
    {
        try
        {
            return new BigInteger(tokenId, Character.MAX_RADIX);
        }
        catch (Exception e)
        {
            return BigInteger.ZERO;
        }
    }

    public void setTokenId(String tokenId)
    {
        this.tokenId = tokenId;
    }

    public String getFunctionId()
    {
        return functionId;
    }

    public void setFunctionId(String functionId)
    {
        this.functionId = functionId;
    }

    public String getResult()
    {
        return result;
    }

    public void setResult(String result)
    {
        this.result = result;
    }

    public long getResultTime()
    {
        return resultTime;
    }

    public void setResultTime(long resultTime)
    {
        this.resultTime = resultTime;
    }

    public String getAddress()
    {
        return instanceKey.split("-")[0];
    }

    public String getTokenAddress()
    {
        return tokenAddress;
    }

    public void setTokenAddress(String address)
    {
        tokenAddress = address;
    }
}
