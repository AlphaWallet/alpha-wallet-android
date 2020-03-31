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
    public String contractInterface = null;
    public Map<Integer, List<String>> addresses = new HashMap<>();
    public Map<String, Module> eventModules = null;

    public boolean hasContract(int chainId, String address)
    {
        if (addresses == null) return false;
        List<String> addrs = addresses.get(chainId);
        return addrs != null && addrs.contains(address);
    }
}
