package io.stormbird.wallet.entity;

/**
 * Created by James on 2/02/2019.
 * Stormbird in Singapore
 */
public class ContractResult
{
    public String name;
    public int chainId;

    public ContractResult(String n, int chain)
    {
        name = n;
        chainId = chain;
    }
}
