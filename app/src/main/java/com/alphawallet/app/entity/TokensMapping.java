package com.alphawallet.app.entity;

import com.alphawallet.app.entity.tokendata.TokenGroup;
import com.alphawallet.token.entity.ContractAddress;

import java.util.List;

public class TokensMapping {

    private List<ContractAddress> contracts = null;
    private String group;

    public List<ContractAddress> getContracts() {
        return contracts;
    }

    public void setContracts(List<ContractAddress> contracts) {
        this.contracts = contracts;
    }

    public TokenGroup getGroup() {
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
        }
    }
}
