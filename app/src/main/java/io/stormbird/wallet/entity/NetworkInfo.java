package io.stormbird.wallet.entity;

public class NetworkInfo {
    public final String name;
    public final String symbol;
    public final String rpcServerUrl;
    public final String etherscanUrl;
    public final int chainId;
    public final boolean isMainNetwork;
    public final String backupNodeUrl;
    public final String etherscanTxUrl;

    public NetworkInfo(
            String name,
            String symbol,
            String rpcServerUrl,
            String etherscanUrl,
            int chainId,
            boolean isMainNetwork) {
        this.name = name;
        this.symbol = symbol;
        this.rpcServerUrl = rpcServerUrl;
        this.etherscanUrl = etherscanUrl;
        this.chainId = chainId;
        this.isMainNetwork = isMainNetwork;
        this.backupNodeUrl = null;
        this.etherscanTxUrl = null;
    }

    public NetworkInfo(
            String name,
            String symbol,
            String rpcServerUrl,
            String etherscanUrl,
            int chainId,
            boolean isMainNetwork,
            String backupNodeUrl,
            String etherscanTxUrl) {
        this.name = name;
        this.symbol = symbol;
        this.rpcServerUrl = rpcServerUrl;
        this.etherscanUrl = etherscanUrl;
        this.chainId = chainId;
        this.isMainNetwork = isMainNetwork;
        this.backupNodeUrl = backupNodeUrl;
        this.etherscanTxUrl = etherscanTxUrl;
    }
}
