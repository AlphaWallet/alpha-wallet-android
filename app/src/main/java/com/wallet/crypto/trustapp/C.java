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

    public static final String EXTRA_ADDRESS = "ADDRESS";
    public static final String EXTRA_CONTRACT_ADDRESS = "CONTRACT_ADDRESS";
    public static final String EXTRA_DECIMALS = "DECIMALS";
    public static final String EXTRA_SYMBOL = "SYMBOL";
    public static final String EXTRA_SENDING_TOKENS = "SENDING_TOKENS";
    public static final String EXTRA_TO_ADDRESS = "TO_ADDRESS";
    public static final String EXTRA_AMOUNT = "AMOUNT";
    public static final String DEFAULT_GAS_PRICE = "30000000000";
    public static final String DEFAULT_GAS_LIMIT = "90000";

    public static final String COINBASE_WIDGET_CODE = "88d6141a-ff60-536c-841c-8f830adaacfd";
    public static final String SHAPESHIFT_KEY = "c4097b033e02163da6114fbbc1bf15155e759ddfd8352c88c55e7fef162e901a800e7eaecf836062a0c075b2b881054e0b9aa2324be7bc3694578493faf59af4";
    public static final String CHANGELLY_REF_ID = "968d4f0f0bf9";

    public interface ErrorCode {

		int UNKNOWN = 1;
		int CANT_GET_STORE_PASSWORD = 2;
	}

    public interface Key {
	    String WALLET = "wallet";
        String TRANSACTION = "transaction";
        String SHOULD_SHOW_SECURITY_WARNING = "should_show_security_warning";
    }

}
