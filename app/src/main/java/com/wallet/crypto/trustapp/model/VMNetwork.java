package com.wallet.crypto.trustapp.model;

/**
 * Created by marat on 10/8/17.
 */

public class VMNetwork {
    private String name;
    private String infuraUrl;
    private String etherscanUrl;
    private int chainId;

    public VMNetwork(String name, String infuraUrl, String etherscanUrl, int chainId) {
        this.name = name;
        this.infuraUrl = infuraUrl;
        this.etherscanUrl = etherscanUrl;
        this.chainId = chainId;
    }


    public String getName() {
        return name;
    }

    public String getInfuraUrl() {
        return infuraUrl;
    }

    public String getEtherscanUrl() {
        return etherscanUrl;
    }

    public int getChainId() {
        return chainId;
    }
}
