package com.wallet.crypto.trustapp.model;

/**
 * Created by marat on 10/8/17.
 */

public class VMNetwork {
    private String name;
    private String symbol;
    private String rpcServerUrl;
    private String backendUrl;
    private String etherscanUrl;
    private int chainId;

    public VMNetwork(String name, String symbol, String rpcServerUrl, String backendUrl, String etherscanUrl, int chainId) {
        this.name = name;
        this.symbol = symbol;
        this.rpcServerUrl = rpcServerUrl;
        this.backendUrl = backendUrl;
        this.etherscanUrl = etherscanUrl;
        this.chainId = chainId;
    }


    public String getName() {
        return name;
    }

    public String getSymbol() { return symbol; }

    public String getRpcUrl() {
        return rpcServerUrl;
    }

    public String getBackendUrl() {
        return backendUrl;
    }

    public String getEtherscanUrl() { return etherscanUrl; }

    public int getChainId() {
        return chainId;
    }
}
