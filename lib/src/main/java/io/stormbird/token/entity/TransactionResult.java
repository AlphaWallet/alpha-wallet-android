package io.stormbird.token.entity;

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

    public boolean needsUpdating()
    {
        return ((System.currentTimeMillis() - resultTime) > 60 * 1000 * 10); //10 minutes: TODO: determine this from function volatility hint
    }
}
