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
    public final int contractChainId;
    public String result;
    public long resultTime;
    public final String attrId;

    public TransactionResult(int chainId, String address, BigInteger tokenId, AttributeType attr)
    {
        this.contractAddress = address;
        this.contractChainId = chainId;
        this.tokenId = tokenId;
        this.method = attr.function.method;
        this.attrId = attr.id;
        result = null;
        resultTime = 0;
    }

    public boolean needsUpdating(long lastTxCheck)
    {
        return (lastTxCheck == 0 || (resultTime - lastTxCheck) <= 0);
    }
}
