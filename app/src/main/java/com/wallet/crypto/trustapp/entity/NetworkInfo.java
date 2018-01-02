package com.wallet.crypto.trustapp.entity;

public class NetworkInfo {
    public final String name;
    public final String symbol;
    public final String rpcServerUrl;
    public final String backendUrl;
    public final String etherscanUrl;
    public final String ticker;
    public final int chainId;
    public final boolean isMainNetwork;

    public NetworkInfo(
            String name,
            String symbol,
            String rpcServerUrl,
            String backendUrl,
            String etherscanUrl,
            String ticker,
            int chainId,
            boolean isMainNetwork) {
        this.name = name;
        this.symbol = symbol;
        this.rpcServerUrl = rpcServerUrl;
        this.backendUrl = backendUrl;
        this.etherscanUrl = etherscanUrl;
        this.ticker = ticker;
        this.chainId = chainId;
        this.isMainNetwork = isMainNetwork;
    }
}
