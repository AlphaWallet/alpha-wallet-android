package com.alphawallet.app.entity;

import com.alphawallet.app.entity.tokens.Token;

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

    public boolean equals(Token token)
    {
        return (token != null && name != null && name.equalsIgnoreCase(token.getAddress()) && chainId == token.tokenInfo.chainId);
    }

    /* replace this with a one-liner use of stream when we up our minSdkVersion to 24 */
    public static ContractResult[] fromAddresses(String[] addresses, int chainID) {
        ContractResult[] retval = new ContractResult[addresses.length];
        for (int i=0; i<addresses.length; i++) {
            retval[i] = new ContractResult(addresses[i], chainID);
        }
        return retval;
    }

}
