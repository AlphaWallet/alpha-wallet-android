package io.awallet.crypto.alphawallet.entity;

public class NetworkInfo {
    public final String name;
    public final String symbol;
    public final String rpcServerUrl;
    public final String backendUrl;
    public final String etherscanUrl;
    public final int chainId;
    public final boolean isMainNetwork;
    public final String backupNodeUrl;

    public NetworkInfo(
            String name,
            String symbol,
            String rpcServerUrl,
            String backendUrl,
            String etherscanUrl,
            int chainId,
            boolean isMainNetwork) {
        this.name = name;
        this.symbol = symbol;
        this.rpcServerUrl = rpcServerUrl;
        this.backendUrl = backendUrl;
        this.etherscanUrl = etherscanUrl;
        this.chainId = chainId;
        this.isMainNetwork = isMainNetwork;
        this.backupNodeUrl = null;
    }

    public NetworkInfo(
            String name,
            String symbol,
            String rpcServerUrl,
            String backendUrl,
            String etherscanUrl,
            int chainId,
            boolean isMainNetwork,
            String backupNodeUrl) {
        this.name = name;
        this.symbol = symbol;
        this.rpcServerUrl = rpcServerUrl;
        this.backendUrl = backendUrl;
        this.etherscanUrl = etherscanUrl;
        this.chainId = chainId;
        this.isMainNetwork = isMainNetwork;
        this.backupNodeUrl = backupNodeUrl;
    }
}
