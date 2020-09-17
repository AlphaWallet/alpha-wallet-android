package com.alphawallet.app.entity;

/**
 * Created by James on 18/03/2019.
 * Stormbird in Singapore
 */
public class UnknownToken
{
    public int chainId;
    public String name;
    public String address;
    public boolean isPopular;

    public UnknownToken(int chainId, String address, boolean isPopular)
    {
        this.chainId = chainId;
        this.address = address;
        this.isPopular = isPopular;
    }
}
