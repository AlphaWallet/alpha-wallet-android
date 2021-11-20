package com.alphawallet.token.entity;

import java.math.BigInteger;

/**
 * Created by James on 7/05/2019.
 * Stormbird in Sydney
 */
public class TransactionResult
{
    public final BigInteger tokenId;
    public final String method;
    public final String contractAddress;
    public final long contractChainId;
    public String result;
    public long resultTime;
    public final String attrId;

    public TransactionResult(long chainId, String address, BigInteger tokenId, Attribute attr)
    {
        this.contractAddress = address;
        this.contractChainId = chainId;
        this.tokenId = tokenId;
        if (attr.function != null) this.method = attr.function.method;
        else if (attr.event != null) this.method = attr.name; //for event store attribute name
        else this.method = attr.label;
        this.attrId = attr.name;
        result = null;
        resultTime = 0;
    }

    public boolean needsUpdating(long lastTxTime)
    {
        //if contract had new transactions then update, or if last tx was -1 (always check)
        return (resultTime == 0 || ((System.currentTimeMillis() + 5*10*1000) < resultTime) || lastTxTime < 0 || lastTxTime > resultTime);
    }
}
