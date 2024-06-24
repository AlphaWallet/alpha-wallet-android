package com.langitwallet.app.entity;

import com.alphawallet.token.entity.ContractAddress;
import com.langitwallet.app.entity.tokendata.TokenGroup;

import java.util.List;

public class TokensMapping
{

    private List<ContractAddress> contracts = null;
    private String group;

    public List<ContractAddress> getContracts()
    {
        return contracts;
    }

    public void setContracts(List<ContractAddress> contracts)
    {
        this.contracts = contracts;
    }

    public TokenGroup getGroup()
    {
        if (group == null) return TokenGroup.ASSET;

        switch (group)
        {
            default:
            case "Assets":
                return TokenGroup.ASSET;
            case "Governance":
                return TokenGroup.GOVERNANCE;
            case "DeFi":
                return TokenGroup.DEFI;
            case "Spam":
                return TokenGroup.SPAM;
        }
    }

    public void setGroup(String group)
    {
        this.group = group;
    }
}
