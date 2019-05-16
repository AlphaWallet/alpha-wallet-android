package io.stormbird.token.entity;

/**
 * Created by James on 15/05/2019.
 * Stormbird in Sydney
 */
public class ContractAddress
{
    public final int chainId;
    public final String address;

    public ContractAddress(int chainId, String address)
    {
        this.chainId = chainId;
        this.address = address;
    }
}
