package com.alphawallet.token.entity;

import java.util.List;

/**
 * Created by James on 15/05/2019.
 * Stormbird in Sydney
 */
public class ContractAddress
{
    public final int chainId;
    public final String address;
    public final boolean isEnabled;

    public ContractAddress(int chainId, String address)
    {
        this.chainId = chainId;
        this.address = address;
        this.isEnabled = true;
    }

    public ContractAddress(int chainId, String address, boolean enabled)
    {
        this.chainId = chainId;
        this.address = address;
        this.isEnabled = enabled;
    }

    //TODO: Only allow FunctionDefinition to have one contract
    public ContractAddress(FunctionDefinition fd)
    {
        this.chainId = fd.contract.addresses.keySet().iterator().next();
        this.address = fd.contract.addresses.get(chainId).iterator().next();
        this.isEnabled = true;
    }
}
