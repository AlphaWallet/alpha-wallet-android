package com.alphawallet.app;

public abstract class C {

    public static final int IMPORT_REQUEST_CODE = 1001;
    public static final int EXPORT_REQUEST_CODE = 1002;
    public static final int SHARE_REQUEST_CODE = 1003;
    public static final int REQUEST_SELECT_NETWORK = 1010;
    public static final int REQUEST_BACKUP_WALLET = 1011;
    public static final int REQUEST_TRANSACTION_CALLBACK = 1012;

    public static final String ETHEREUM_NETWORK_NAME = "Ethereum";
    public static final String CLASSIC_NETWORK_NAME = "Ethereum Classic";
    public static final String POA_NETWORK_NAME = "POA";
    public static final String XDAI_NETWORK_NAME = "xDai";
    public static final String KOVAN_NETWORK_NAME = "Kovan (Test)";
    public static final String ROPSTEN_NETWORK_NAME = "Ropsten (Test)";
    public static final String SOKOL_NETWORK_NAME = "Sokol (Test)";
    public static final String RINKEBY_NETWORK_NAME = "Rinkeby (Test)";
    public static final String GOERLI_NETWORK_NAME = "Görli (Test)";
    public static final String ARTIS_SIGMA1_NETWORK = "ARTIS sigma1";
    public static final String ARTIS_TAU1_NETWORK = "ARTIS tau1 (Test)";

    public static final String ETHEREUM_TICKER_NAME = "ethereum";
    public static final String CLASSIC_TICKER_NAME = "ethereum-classic";
    public static final String XDAI_TICKER_NAME = "dai";
    public static final String ARTIS_SIGMA_TICKER = "artis";

    public static final String ETHEREUM_TICKER = "ethereum";
    public static final String POA_TICKER = "poa";

    public static final String USD_SYMBOL = "$";
    public static final String ETH_SYMBOL = "ETH";
    public static final String xDAI_SYMBOL = "xDai";
    public static final String POA_SYMBOL = "POA";
    public static final String ETC_SYMBOL = "ETC";
    public static final String GOERLI_SYMBOL = "GÖETH";
    public static final String ARTIS_SIGMA1_SYMBOL = "ATS";
    public static final String ARTIS_TAU1_SYMBOL = "ATS";

    public static final String BURN_ADDRESS = "0x0000000000000000000000000000000000000000";
    public static final String ENSCONTRACT = "0x314159265dD8dbb310642f98f50C066173C1259b";

    public static final String GWEI_UNIT = "Gwei";

    public static final String MARKET_SALE = "market";

    public static final String EXTRA_ADDRESS = "ADDRESS";
    public static final String EXTRA_CONTRACT_ADDRESS = "CONTRACT_ADDRESS";
    public static final String EXTRA_DECIMALS = "DECIMALS";
    public static final String EXTRA_SYMBOL = "SYMBOL";
    public static final String EXTRA_SENDING_TOKENS = "SENDING_TOKENS";
    public static final String EXTRA_TO_ADDRESS = "TO_ADDRESS";
    public static final String EXTRA_AMOUNT = "AMOUNT";
    public static final String EXTRA_GAS_PRICE = "GAS_PRICE";
    public static final String EXTRA_GAS_LIMIT = "GAS_LIMIT";
    public static final String EXTRA_CONTRACT_NAME = "NAME";
    public static final String EXTRA_TOKEN_ID = "TID";
    public static final String EXTRA_TOKEN_BALANCE = "BALANCE";
    public static final String EXTRA_TOKENID_LIST = "TOKENIDLIST";
    public static final String ERC875RANGE = "ERC875RANGE";
    public static final String TOKEN_TYPE = "TOKEN_TYPE";
    public static final String MARKET_INSTANCE = "MARKET_INSTANCE";
    public static final String IMPORT_STRING = "TOKEN_IMPORT";
    public static final String EXTRA_PRICE = "TOKEN_PRICE";
    public static final String EXTRA_STATE = "TRANSFER_STATE";
    public static final String EXTRA_WEB3TRANSACTION = "WEB3_TRANSACTION";
    public static final String EXTRA_NETWORK_NAME = "NETWORK_NAME";
    public static final String EXTRA_NETWORK_MAINNET = "NETWORK_MAINNET";
    public static final String EXTRA_ENS_DETAILS = "ENS_DETAILS";
    public static final String EXTRA_HAS_DEFINITION = "HAS_TOKEN_DEF";
    public static final String EXTRA_SUCCESS = "TX_SUCCESS";
    public static final String EXTRA_HEXDATA = "TX_HEX";
    public static final String EXTRA_NETWORKID = "NET_ID";
    public static final String EXTRA_TRANSACTION_DATA = "TS_TRANSACTIONDATA";
    public static final String EXTRA_FUNCTION_NAME = "TS_FUNC_NAME";
    public static final String EXTRA_SINGLE_ITEM = "SINGLE_ITEM";
    public static final String EXTRA_CHAIN_ID = "CHAIN_ID";
    public static final String EXTRA_CALLBACKID = "CALLBACK_ID";

    public static final String PRUNE_ACTIVITY =
            "com.stormbird.wallet.PRUNE_ACTIVITY";

    public static final String RESET_WALLET =
            "com.stormbird.wallet.RESET";
    public static final String ADDED_TOKEN =
            "com.stormbird.wallet.ADDED";
    public static final String CHANGED_LOCALE =
            "com.stormbird.wallet.CHANGED_LOCALE";
    public static final String DOWNLOAD_READY =
            "com.stormbird.wallet.DOWNLOAD_READY";
    public static final String PAGE_LOADED =
            "com.stormbird.wallet.PAGE_LOADED";
    public static final String RESET_TOOLBAR =
            "com.stormbird.wallet.RESET_TOOLBAR";
    public static final String SIGN_DAPP_TRANSACTION =
            "com.stormbird.wallet.SIGN_TRANSACTION";
    public static final String REQUEST_NOTIFICATION_ACCESS =
            "com.stormbird.wallet.REQUEST_NOTIFICATION";
    public static final String BACKUP_WALLET_SUCCESS =
            "com.stormbird.wallet.BACKUP_SUCCESS";

    public static final String COINBASE_WIDGET_CODE = "88d6141a-ff60-536c-841c-8f830adaacfd";
    public static final String SHAPESHIFT_KEY = "c4097b033e02163da6114fbbc1bf15155e759ddfd8352c88c55e7fef162e901a800e7eaecf836062a0c075b2b881054e0b9aa2324be7bc3694578493faf59af4";
    public static final String CHANGELLY_REF_ID = "968d4f0f0bf9";
    public static final String DONATION_ADDRESS = "0xb1aD48527d694D30401D082bcD21a33F41811501";

    public static final String DEFAULT_GAS_PRICE = "30000000000";
    public static final String DEFAULT_XDAI_GAS_PRICE = "1000000000";
    public static final String DEFAULT_GAS_LIMIT = "90000";
    public static final String DEFAULT_GAS_LIMIT_FOR_TOKENS = "144000";
    public static final String DEFAULT_GAS_LIMIT_FOR_NONFUNGIBLE_TOKENS = "432000"; //NFT's typically require more gas
    public static final String DEFAULT_GAS_LIMIT_FOR_END_CONTRACT = "200000"; //TODO: determine appropriate gas limit for contract destruct
    public static final long GAS_PER_BYTE = 310; //from experimentation
    public static final long GAS_LIMIT_MIN = 21000L;
    public static final long GAS_LIMIT_MAX = 900000L;
    public static final long GAS_PRICE_MIN = 1000000000L;
    public static final long NETWORK_FEE_MAX = 90000000000000000L;
    public static final int ETHER_DECIMALS = 18;

    //FOR DEMOS ETC
    public static final boolean SHOW_NEW_ACCOUNT_PROMPT = false;   //this will switch off the splash screen 'please make a key' message

    public static final String DEFAULT_NETWORK = ETHEREUM_NETWORK_NAME;

    public static final String TELEGRAM_PACKAGE_NAME = "org.telegram.messenger";
    public static final String TWITTER_PACKAGE_NAME = "com.twitter.android";
    public static final String FACEBOOK_PACKAGE_NAME = "com.facebook.katana";
    public static final String AWALLET_TELEGRAM_URL = "https://t.me/AlphaWalletGroup";
    public static final String AWALLET_TWITTER_ID = "twitter://user?user_id=938624096123764736";
    public static final String AWALLET_FACEBOOK_ID = "fb://page/1958651857482632";
    public static final String AWALLET_TWITTER_URL = "https://twitter.com/AlphaWallet";
    public static final String AWALLET_FACEBOOK_URL = "https://www.facebook.com/AlphaWallet/";

    public interface ErrorCode {
        int UNKNOWN = 1;
        int CANT_GET_STORE_PASSWORD = 2;
        int ALREADY_ADDED = 3;
        int EMPTY_COLLECTION = 4;
    }

    public interface Key {
        String WALLET = "wallet";
        String TRANSACTION = "transaction";
        String TICKET = "ticket";
        String TICKET_RANGE = "ticket_range";
        String MARKETPLACE_EVENT = "marketplace_event";
        String SHOULD_SHOW_SECURITY_WARNING = "should_show_security_warning";
        String FROM_SETTINGS = "from_settings";
    }

    public static final String DAPP_LASTURL_KEY = "dappURL";
    public static final String DAPP_BROWSER_HISTORY = "DAPP_BROWSER_HISTORY";
    public static final String DAPP_BROWSER_BOOKMARKS = "dappBrowserBookmarks";
    public static final String DAPP_DEFAULT_URL = "https://www.stateofthedapps.com/";

    public static final String ENS_SCAN_BLOCK = "ens_check_block";
    public static final String ENS_HISTORY = "ensHistory";

    public enum TokenStatus {
        DEFAULT, PENDING, INCOMPLETE
    }

    public static final String GOOGLE_SEARCH_PREFIX = "https://www.google.com/search?q=";
    public static final String HTTPS_PREFIX = "https://";

    // Settings Badge Keys
    public static final String KEY_NEEDS_BACKUP = "needsBackup";
}
