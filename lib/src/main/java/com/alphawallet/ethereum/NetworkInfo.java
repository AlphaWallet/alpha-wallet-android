package com.alphawallet.ethereum;

/* it's some kind of Trust Ethereum Wallet naming convention that a
 * class that has no behaviour (equivalent of C's struct) is called
 * SomethingInfo. I didn't agree with this naming convention but I'll
 * keep it here */

public class NetworkInfo {
    public final String name;
    public final String symbol;
    public final String rpcServerUrl;
    public final String etherscanUrl; // This is used by the Transaction Detail page for the user to visit a website with detailed transaction information
    public final long chainId;

    public NetworkInfo(
            String name,
            String symbol,
            String rpcServerUrl,
            String etherscanUrl,
            long chainId) {
        this.name = name;
        this.symbol = symbol;
        this.rpcServerUrl = rpcServerUrl;
        this.etherscanUrl = etherscanUrl;
        this.chainId = chainId;
    }

}
