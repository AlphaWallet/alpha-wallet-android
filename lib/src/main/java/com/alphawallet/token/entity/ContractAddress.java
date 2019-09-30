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

    public ContractAddress(int chainId, String address)
    {
        this.chainId = chainId;
        this.address = address;
    }

    /**
     * Select contract to use from list of allowed contracts.
     * Selection rule:
     * 1 - Is the token itself a viable choice? If so use the token contract
     * 2 - Token contract not available - choose first contract on same chain
     * 3 - Token contract not available and no contract on same chainId as token - must be a cross-chain call, use first contract in list.
     * @param fd
     * @param chainId
     * @param address
     */
    public ContractAddress(FunctionDefinition fd, int chainId, String address)
    {
        List<String> contracts = fd.contract.addresses.get(chainId);
        if (contracts != null && contracts.contains(address))
        {
            this.address = address;
            this.chainId = chainId;
        }
        else if (contracts != null)
        {
            this.chainId = chainId;
            this.address = fd.contract.addresses.get(chainId).iterator().next();
        }
        else
        {
            this.chainId = fd.contract.addresses.keySet().iterator().next();
            this.address = fd.contract.addresses.get(chainId).iterator().next();
        }
    }

    public ContractAddress(FunctionDefinition fd)
    {
        this.chainId = fd.contract.addresses.keySet().iterator().next();
        this.address = fd.contract.addresses.get(chainId).iterator().next();
    }
}
