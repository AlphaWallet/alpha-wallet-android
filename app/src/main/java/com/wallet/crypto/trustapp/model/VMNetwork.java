package com.wallet.crypto.trustapp.model;

/**
 * Created by marat on 10/8/17.
 */

public class VMNetwork {
    private String name;
    private String symbol;
    private String rpcServerUrl;
    private String backendUrl;
    private int chainId;

    public VMNetwork(String name, String symbol, String rpcServerUrl, String backendUrl, int chainId) {
        this.name = name;
        this.symbol = symbol;
        this.rpcServerUrl = rpcServerUrl;
        this.backendUrl = backendUrl;
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

    public int getChainId() {
        return chainId;
    }
}
