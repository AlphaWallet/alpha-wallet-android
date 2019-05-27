package io.stormbird.token.entity;

import java.util.List;
import java.util.Map;

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

    public ContractAddress(FunctionDefinition fd, int chainId, String address)
    {
        this.chainId = chainId;
        List<String> contracts = fd.contract.addresses.get(chainId);
        if (contracts.contains(address))
        {
            this.address = address;
        }
        else
        {
            if (fd.contract.addresses.get(chainId) == null) this.address = "0x";
            else this.address = fd.contract.addresses.get(chainId).get(0);
        }
    }
}
