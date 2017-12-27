package com.wallet.crypto.trustapp;

public abstract class C {

    public static final int IMPORT_REQUEST_CODE = 1001;
    public static final int EXPORT_REQUEST_CODE = 1002;
    public static final int SHARE_REQUEST_CODE = 1003;

    public static final long ETHER_DECIMALS = 18;

    public static final String ETHEREUM_NETWORK_NAME = "Ethereum";
    public static final String POA_NETWORK_NAME = "POA Network";
    public static final String KOVAN_NETWORK_NAME = "Kovan (Test)";
    public static final String ROPSTEN_NETWORK_NAME = "Ropsten (Test)";

    public static final String ETHEREUM_TIKER = "ethereum";
    public static final String POA_TIKER = "poa";

    public static final String USD_SYMBOL = "$";
    public static final String ETH_SYMBOL = "ETH";
    public static final String POA_SYMBOL = "POA";

    public interface ErrorCode {

		int UNKNOWN = 1;
		int CANT_GET_STORE_PASSWORD = 2;
	}

    public interface Key {
	    String WALLET = "wallet";
        String TRANSACTION = "transaction";
    }
}
