package com.alphawallet.app.entity;

/**
 * Created by James on 18/03/2019.
 * Stormbird in Singapore
 */
public class UnknownToken
{
    public int chainId;
    public String address;

    public UnknownToken(int chainId, String address)
    {
        this.chainId = chainId;
        this.address = address;
    }
}
