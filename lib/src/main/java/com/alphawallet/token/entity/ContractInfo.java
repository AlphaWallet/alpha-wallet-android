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
    public final Map<Integer, List<String>> addresses = new HashMap<>();

    public ContractInfo(String contractType, Map<Integer, List<String>> addresses)
    {
        this.contractInterface = contractType;
        this.addresses.putAll(addresses);
    }

    public ContractInfo(String contractType)
    {
        this.contractInterface = contractType;
    }
}
