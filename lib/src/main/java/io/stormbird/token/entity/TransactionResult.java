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

    public TransactionResult(int chainId, String address, BigInteger tokenId, String method)
    {
        this.contractAddress = address;
        this.contractChainId = chainId;
        this.tokenId = tokenId;
        this.method = method;
        result = null;
    }
}
