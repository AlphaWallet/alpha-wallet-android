package com.alphawallet.token.entity;

/**
 * Created by James on 15/05/2019.
 * Stormbird in Sydney
 */
public class ContractAddress
{
    public final long chainId;
    public final String address;

    public ContractAddress(long chainId, String address)
    {
        this.chainId = chainId;
        this.address = address;
    }

    public ContractAddress(String chainAddr)
    {
        String[] sp = chainAddr.split("-");
        this.address = sp[0];
        this.chainId = Long.parseLong(sp[1]);
    }

    public String getAddressKey()
    {
        return address.toLowerCase() + "-" + chainId;
    }

    //TODO: Only allow FunctionDefinition to have one contract
    public ContractAddress(FunctionDefinition fd)
    {
        this.chainId = fd.contract.addresses.keySet().iterator().next();
        this.address = fd.contract.addresses.get(chainId).iterator().next();
    }
}
