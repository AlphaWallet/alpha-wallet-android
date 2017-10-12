package com.wallet.crypto.trust.model;

/**
 * Created by marat on 10/8/17.
 */

public class VMNetwork {
    private String name;
    private String infuraUrl;
    private String etherscanUrl;
    private String etherscanApiKey;
    private int chainId;

    public VMNetwork(String name, String infuraUrl, String etherscanUrl, String etherscanApiKey, int chainId) {
        this.name = name;
        this.infuraUrl = infuraUrl;
        this.etherscanUrl = etherscanUrl;
        this.etherscanApiKey = etherscanApiKey;
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

    public String getEtherscanApiKey() {
        return etherscanApiKey;
    }

    public int getChainId() {
        return chainId;
    }
}
