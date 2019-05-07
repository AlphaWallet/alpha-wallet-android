package io.stormbird.wallet.entity;

import java.math.BigInteger;

/**
 * Created by James on 7/05/2019.
 * Stormbird in Sydney
 */
public class TransactionResult
{
    public final BigInteger tokenId;
    public final String method;
    public final Token token;
    public String result;
    public long resultTime;

    public TransactionResult(Token token, BigInteger tokenId, String method)
    {
        this.token = token;
        this.tokenId = tokenId;
        this.method = method;
        result = null;
    }
}
