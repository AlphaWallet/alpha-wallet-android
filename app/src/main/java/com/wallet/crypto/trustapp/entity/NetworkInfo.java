package com.wallet.crypto.trustapp.entity;

public class NetworkInfo {
    public final String name;
    public final String infuraUrl;
    public final String etherScanUrl;
    public final String etherScanApiKey;
    public final int chainId;

    public NetworkInfo(String name, String infuraUrl, String etherScanUrl, String etherScanApiKey, int chainId) {
        this.name = name;
        this.infuraUrl = infuraUrl;
        this.etherScanUrl = etherScanUrl;
        this.etherScanApiKey = etherScanApiKey;
        // this.blockExplorerCredentials = blockExplorerCredentials
	    /*
	     * TODO: public class {
	     *      public final String url;
	     *      public final String apiKey;
	     *      public final String name;
	     * }
	     */
        this.chainId = chainId;
    }
}
