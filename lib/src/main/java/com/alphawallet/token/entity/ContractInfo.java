package com.alphawallet.token.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by James on 2/05/2019.
 * Stormbird in Sydney
 */
public class ContractInfo
{
    public final String contractInterface;
    public final Map<Long, List<String>> addresses = new HashMap<>();

    public ContractInfo(String contractType, Map<Long, List<String>> addresses)
    {
        this.contractInterface = contractType;
        this.addresses.putAll(addresses);
    }

    public ContractInfo(String contractType)
    {
        this.contractInterface = contractType;
    }

    public boolean hasContractTokenScript(long chainId, String address)
    {
        List<String> addrs = addresses.get(chainId);
        return addrs != null && addrs.contains(address);
    }

    public long getfirstChainId()
    {
        if (addresses.keySet().size() > 0) return addresses.keySet().iterator().next();
        else return 0;
    }

    public String getFirstAddress()
    {
        long chainId = getfirstChainId();
        return addresses.get(chainId).size() > 0 ? addresses.get(chainId).get(0) : "";
    }
}