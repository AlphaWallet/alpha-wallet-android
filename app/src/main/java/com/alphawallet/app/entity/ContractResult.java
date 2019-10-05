package com.alphawallet.app.entity;

import java.util.List;

/**
 * Created by James on 2/02/2019.
 * Stormbird in Singapore
 */
public class ContractResult
{
    public String name;
    public int chainId;
    public ContractType type;

    public ContractResult(String n, int chain)
    {
        name = n;
        chainId = chain;
        type = null;
    }

    public ContractResult(String n, int chain, ContractType t)
    {
        name = n;
        chainId = chain;
        type = t;
    }

    public static void addIfNotInList(List<ContractResult> contractList, ContractResult candidate)
    {
        boolean inList = false;
        for (ContractResult r : contractList)
        {
            if (r.name.equals(candidate.name) && r.chainId == candidate.chainId)
            {
                inList = true;
                break;
            }
        }

        if (!inList)
        {
            contractList.add(candidate);
        }
    }
}
