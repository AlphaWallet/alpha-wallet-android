package com.alphawallet.app.entity;

public class NetworkInfo extends com.alphawallet.ethereum.NetworkInfo {
    public String backupNodeUrl = null;
    public String etherscanTxUrl = null; //This is used by the API call to fetch transactions

    public NetworkInfo(
            String name,
            String symbol,
            String rpcServerUrl,
            String etherscanUrl,
            int chainId,
            boolean isMainNetwork,
            String backupNodeUrl,
            String etherscanTxUrl) {
        super(name, symbol, rpcServerUrl, etherscanUrl, chainId, isMainNetwork);
        this.backupNodeUrl = backupNodeUrl;
        this.etherscanTxUrl = etherscanTxUrl;
    }

    public String getShortName()
    {
        int index = this.name.indexOf(" (Test)");
        if (index > 0) return this.name.substring(0, index);
        else if (this.name.length() > 10) return this.symbol;
        else return this.name;
    }
}
