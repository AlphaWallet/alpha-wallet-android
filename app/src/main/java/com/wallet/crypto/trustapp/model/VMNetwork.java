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
    private String ticker;
    private int chainId;

    public VMNetwork(String name, String symbol, String rpcServerUrl, String backendUrl, String etherscanUrl, String ticker, int chainId) {
        this.name = name;
        this.symbol = symbol;
        this.rpcServerUrl = rpcServerUrl;
        this.backendUrl = backendUrl;
        this.etherscanUrl = etherscanUrl;
        this.ticker = ticker;
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

    public String getTicker() { return ticker; }

    public int getChainId() {
        return chainId;
    }
}
