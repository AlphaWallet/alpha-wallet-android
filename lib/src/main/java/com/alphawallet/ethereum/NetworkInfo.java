package com.alphawallet.ethereum;

/* it's some kind of Trust Ethereum Wallet naming convention that a
 * class that has no behaviour (equivalent of C's struct) is called
 * SomethingInfo. I didn't agree with this naming convention but I'll
 * keep it here */

public class NetworkInfo {
    public final String name;
    public final String symbol;
    public final String rpcServerUrl;
    public final String etherscanUrl;
    public final int chainId;
    public final boolean isMainNetwork;
    public final String tickerId;
    public final String blockscoutAPI;

    public NetworkInfo(
            String name,
            String symbol,
            String rpcServerUrl,
            String etherscanUrl,
            int chainId,
            boolean isMainNetwork,
            String tickerId,
            String blockscoutAPI) {
        this.name = name;
        this.symbol = symbol;
        this.rpcServerUrl = rpcServerUrl;
        this.etherscanUrl = etherscanUrl;
        this.chainId = chainId;
        this.isMainNetwork = isMainNetwork;
        this.tickerId = tickerId;
        this.blockscoutAPI = blockscoutAPI;
    }

}
