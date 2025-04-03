package com.alphawallet.app;

public abstract class C {

    public static final int IMPORT_REQUEST_CODE = 1001;
    public static final int EXPORT_REQUEST_CODE = 1002;
    public static final int SHARE_REQUEST_CODE = 1003;
    public static final int REQUEST_SELECT_NETWORK = 1010;
    public static final int REQUEST_BACKUP_WALLET = 1011;
    public static final int REQUEST_TRANSACTION_CALLBACK = 1012;
    public static final int UPDATE_LOCALE = 1013;
    public static final int UPDATE_CURRENCY = 1014;
    public static final int REQUEST_UNIVERSAL_SCAN = 1015;
    public static final int TOKEN_SEND_ACTIVITY = 1016;

    public static final int BARCODE_READER_REQUEST_CODE = 1;
    public static final int SET_GAS_SETTINGS = 2;
    public static final int COMPLETED_TRANSACTION = 3;
    public static final int SEND_INTENT_REQUEST_CODE = 4;
    public static final int TERMINATE_ACTIVITY = 5;
    public static final int ADDED_TOKEN_RETURN = 9;
    public static final int STANDARD_POPUP_INACTIVITY_DISMISS = 15*1000; //Standard dismiss after 15 seconds

    public static final String ETHEREUM_NETWORK_NAME = "Ethereum";
    public static final String CLASSIC_NETWORK_NAME = "Ethereum Classic";
    public static final String XDAI_NETWORK_NAME = "Gnosis";
    public static final String GOERLI_NETWORK_NAME = "Görli (Test)";
    public static final String BINANCE_TEST_NETWORK = "BSC TestNet";
    public static final String BINANCE_MAIN_NETWORK = "Binance (BSC)";
    public static final String FANTOM_NETWORK = "Fantom Opera";
    public static final String FANTOM_TEST_NETWORK = "Fantom (Test)";
    public static final String AVALANCHE_NETWORK = "Avalanche";
    public static final String FUJI_TEST_NETWORK = "Avalanche FUJI (Test)";
    public static final String POLYGON_NETWORK = "Polygon";
    public static final String POLYGON_TEST_NETWORK = "Mumbai (Test)";
    public static final String OPTIMISTIC_NETWORK = "Op Mainnet";
    public static final String CRONOS_MAIN_NETWORK = "Cronos";
    public static final String CRONOS_TEST_NETWORK = "Cronos (Test)";
    public static final String ARBITRUM_ONE_NETWORK = "Arbitrum One";
    public static final String PALM_NAME = "PALM";
    public static final String PALM_TEST_NAME = "PALM (Test)";
    public static final String KLAYTN_NAME = "Kaia Mainnet";
    public static final String KLAYTN_BAOBAB_NAME = "Kaia Kairos (Test)";
    public static final String IOTEX_NAME = "IoTeX";
    public static final String IOTEX_TESTNET_NAME = "IoTeX (Test)";
    public static final String AURORA_MAINNET_NAME = "Aurora";
    public static final String AURORA_TESTNET_NAME = "Aurora (Test)";
    public static final String MILKOMEDA_NAME = "Milkomeda Cardano";
    public static final String MILKOMEDA_TESTNET_NAME = "Milkomeda Cardano (Test)";
    public static final String SEPOLIA_TESTNET_NAME = "Sepolia (Test)";
    public static final String ARBITRUM_TESTNET_NAME = "Arbitrum Sepolia (Test)";
    public static final String OKX_NETWORK_NAME = "OKXChain Mainnet";
    public static final String ROOTSTOCK_NETWORK_NAME = "Rootstock";
    public static final String ROOTSTOCK_TESTNET_NAME = "Rootstock (Test)";
    public static final String LINEA_NAME = "Linea";
    public static final String LINEA_TESTNET_NAME = LINEA_NAME + " (Test)";
    public static final String HOLESKY_TESTNET_NAME = "Holesky (Test)";

    public static final String AMOY_TESTNET_NAME = "Amoy (Test)";
    public static final String BASE_MAINNET_NAME = "Base";
    public static final String BASE_TESTNET_NAME = "Base Sepolia (Test)";
    public static final String MANTLE_MAINNET_NAME = "Mantle";
    public static final String MANTLE_TESTNET_NAME = "Mantle Sepolia (Test)";
    public static final String MINT_MAINNET_NAME = "Mint";
    public static final String MINT_TESTNET_NAME = "Mint Sepolia (Test)";

    public static final String ETHEREUM_TICKER_NAME = "ethereum";
    public static final String CLASSIC_TICKER_NAME = "ethereum-classic";
    public static final String XDAI_TICKER_NAME = "dai";
    public static final String BINANCE_TICKER = "binance";

    public static final String ETHEREUM_TICKER = "ethereum";

    public static final String USD_SYMBOL = "$";
    public static final String ETH_SYMBOL = "ETH";

    public static final String MANTLE_SYMBOL = "MNT";
    public static final String xDAI_SYMBOL = "xDai";
    public static final String ETC_SYMBOL = "ETC";
    public static final String GOERLI_SYMBOL = "GÖETH";
    public static final String BINANCE_SYMBOL = "BNB";
    public static final String FANTOM_SYMBOL = "FTM";
    public static final String AVALANCHE_SYMBOL = "AVAX";
    public static final String POLYGON_SYMBOL = "MATIC";
    public static final String CRONOS_SYMBOL = "CRO";
    public static final String CRONOS_TEST_SYMBOL = "tCRO";
    public static final String ARBITRUM_SYMBOL = "AETH";
    public static final String PALM_SYMBOL = "PALM";
    public static final String KLAYTN_SYMBOL = "KAIA";
    public static final String IOTEX_SYMBOL = "IOTX";
    public static final String MILKOMEDA_SYMBOL = "milkADA";
    public static final String MILKOMEDA_TEST_SYMBOL = "milktADA";
    public static final String SEPOLIA_SYMBOL = "ETH";
    public static final String OKX_SYMBOL = "OKT";
    public static final String ROOTSTOCK_SYMBOL = "RBTC";
    public static final String ROOTSTOCK_TEST_SYMBOL = "tBTC";
    public static final String HOLESKY_TEST_SYMBOL = "Hol" + ETH_SYMBOL;

    public static final String AMOY_TESTNET_SYMBOL = "Am" + ETH_SYMBOL;

    public static final String BURN_ADDRESS = "0x0000000000000000000000000000000000000000";

    //some important known contracts - NB must be all lower case for switch statement
    public static final String DAI_TOKEN = "0x6b175474e89094c44da98b954eedeac495271d0f";
    public static final String SAI_TOKEN = "0x89d24a6b4ccb1b6faa2625fe562bdd9a23260359";

    public static final String ALPHAWALLET_WEB = "https://www.alphawallet.com";

    public static final String XDAI_BRIDGE_DAPP = "https://bridge.xdaichain.com/";

    public static final String QUICKSWAP_EXCHANGE_DAPP = "https://quickswap.exchange/#/swap";
    public static final String ONEINCH_EXCHANGE_DAPP   = "https://app.1inch.io/#/[CHAIN]/swap/[TOKEN1]/[TOKEN2]";

    public static final String GLIDE_URL_INVALID = "com.bumptech.glide.load.HttpException";

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
    public static final String EXTRA_CUSTOM_GAS_LIMIT = "CUSTOM_GAS_LIMIT";
    public static final String EXTRA_GAS_LIMIT_PRESET = "GAS_LIMIT_PRESET";
    public static final String EXTRA_ACTION_NAME = "NAME";
    public static final String EXTRA_TOKEN_ID = "TID";
    public static final String EXTRA_TOKEN_BALANCE = "BALANCE";
    public static final String EXTRA_TOKENID_LIST = "TOKENIDLIST";
    public static final String EXTRA_ATTESTATION_ID = "ATTNID";
    public static final String EXTRA_NFTASSET_LIST = "NFTASSET_LIST";
    public static final String EXTRA_NFTASSET = "NFTASSET";
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
    public static final String EXTRA_LOCALE = "LOCALE_STRING";
    public static final String EXTRA_PAGE_TITLE = "PTITLE";
    public static final String EXTRA_CURRENCY = "CURRENCY_STRING";
    public static final String EXTRA_MIN_GAS_PRICE = "_MINGASPRICE";
    public static final String EXTRA_QR_CODE = "QR_SCAN_CODE";
    public static final String EXTRA_UNIVERSAL_SCAN = "UNIVERSAL_SCAN";
    public static final String EXTRA_NONCE = "_NONCE";
    public static final String EXTRA_TXHASH = "_TXHASH";
    public static final String DAPP_URL_LOAD = "DAPP_URL";
    public static final String EXTRA_PRICE_ALERT = "EXTRA_PRICE_ALERT";
    public static final String EXTRA_SESSION_ID = "SESSION_ID";
    public static final String EXTRA_WC_REQUEST_ID = "REQUEST_ID";
    public static final String EXTRA_APPROVED = "APPROVED";
    public static final String EXTRA_CHAIN_AVAILABLE = "CHAIN_AVAILABLE";
    public static final String EXTRA_NAME = "NAME";
    public static final String EXTRA_CHAIN_OBJ = "CHAIN_OBJ";
    public static final String EXTRA_1559_TX = "1559_TX";
    public static final String EXTRA_FROM_SPLASH = "FROM_SPLASH";

    public static final String PRUNE_ACTIVITY =
            "com.stormbird.wallet.PRUNE_ACTIVITY";

    public static final String RESET_WALLET =
            "com.stormbird.wallet.RESET";
    public static final String ADDED_TOKEN =
            "com.stormbird.wallet.ADDED";
    public static final String CHANGED_LOCALE =
            "com.stormbird.wallet.CHANGED_LOCALE";
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
    public static final String CHANGE_CURRENCY =
            "com.stormbird.wallet.CHANGE_CURRENCY";
    public static final String RESET_TRANSACTIONS =
            "com.stormbird.wallet.RESET_TRANSACTIONS";
    public static final String WALLET_CONNECT_REQUEST =
            "com.stormbird.wallet.WALLET_CONNECT";
    public static final String WALLET_CONNECT_NEW_SESSION =
            "com.stormbird.wallet.WC_NEW_SESSION";
    public static final String WALLET_CONNECT_FAIL =
            "com.stormbird.wallet.WC_FAIL";
    public static final String WALLET_CONNECT_COUNT_CHANGE =
            "com.stormbird.wallet.WC_CCHANGE";
    public static final String WALLET_CONNECT_CLIENT_TERMINATE =
            "com.stormbird.wallet.WC_CLIENT_TERMINATE";
    public static final String WALLET_CONNECT_SWITCH_CHAIN =
            "com.stormbird.wallet.WC_SWITCH_CHAIN";
    public static final String WALLET_CONNECT_ADD_CHAIN =
            "com.stormbird.wallet.WC_ADD_CHAIN";
    public static final String SHOW_BACKUP = "com.stormbird.wallet.CHECK_BACKUP";
    public static final String HANDLE_BACKUP = "com.stormbird.wallet.HANDLE_BACKUP";
    public static final String FROM_HOME_ROUTER = "HomeRouter";
    public static final String TOKEN_CLICK = "com.stormbird.wallet.TOKEN_CLICK";
    public static final String SETTINGS_INSTANTIATED = "com.stormbird.wallet.SETTINGS_INSTANTIATED";
    public static final String APP_FOREGROUND_STATE = "com.alphawallet.APP_FOREGROUND_STATE";
    public static final String EXTRA_APP_FOREGROUND = "com.alphawallet.IS_FOREGORUND";
    public static final String QRCODE_SCAN = "com.alphawallet.QRSCAN";
    public static final String AWALLET_CODE = "com.alphawallet.AWALLET";
    public static final String SIGNAL_NFT_SYNC = "com.alphawallet.SYNC_NFT";
    public static final String SYNC_STATUS = "com.alphawallet.SYNC_STATUS";

    public static final String DEFAULT_GAS_PRICE =     "10000000000";
    public static final String DEFAULT_XDAI_GAS_PRICE = "1000000000";
    public static final String DEFAULT_GAS_LIMIT_FOR_TOKENS = "144000";
    public static final String DEFAULT_UNKNOWN_FUNCTION_GAS_LIMIT = "1000000"; //if we don't know the specific function, we default to 1 million gas limit
    public static final String DEFAULT_GAS_LIMIT_FOR_NONFUNGIBLE_TOKENS = "432000"; //NFTs typically require more gas
    public static final long GAS_LIMIT_MIN = 21000L;
    public static final long GAS_LIMIT_DEFAULT = 90000L;
    public static final long GAS_LIMIT_CONTRACT = 1000000L;
    public static final long GAS_PRICE_MIN = 400000000L;
    public static final int ETHER_DECIMALS = 18;
    public static final long GAS_LIMIT_MAX =          4712380L;  //Max block gas for most chains
    public static final long GAS_LIMIT_MAX_KLAYTN = 100000000L;  //Klaytn gas limit, see https://docs.klaytn.com/klaytn/design/computation/computation-cost
    public static final long GAS_LIMIT_MAX_AURORA = 6721975L;

    //FOR DEMOS ETC
    public static final boolean SHOW_NEW_ACCOUNT_PROMPT = false;   //this will switch off the splash screen 'please make a key' message

    public static final String DEFAULT_NETWORK = ETHEREUM_NETWORK_NAME;

    public static final String TWITTER_PACKAGE_NAME = "com.twitter.android";
    public static final String FACEBOOK_PACKAGE_NAME = "com.facebook.katana";
    public static final String LINKEDIN_PACKAGE_NAME = "com.linkedin.android";
    public static final String REDDIT_PACKAGE_NAME = "com.reddit.frontpage";
    public static final String INSTAGRAM_PACKAGE_NAME = "com.instagram.android";
    public static final String FROM_NOTIFICATION = "from_notification";
    public static final int SHORT_SYMBOL_LENGTH = 5;

    public interface ErrorCode {

        int UNKNOWN = 1;
        int CANT_GET_STORE_PASSWORD = 2;
        int ALREADY_ADDED = 3;
        int EMPTY_COLLECTION = 4;

        // Swap Error Codes
        int INSUFFICIENT_BALANCE = 5;
        int SWAP_CHAIN_ERROR = 6;
        int SWAP_CONNECTIONS_ERROR = 7;
        int SWAP_QUOTE_ERROR = 8;
        int SWAP_TIMEOUT_ERROR = 9;
    }
    public interface Key {

        String WALLET = "wallet";
        String TRANSACTION = "transaction";
        String TICKET_RANGE = "ticket_range";
        String MARKETPLACE_EVENT = "marketplace_event";
        String SHOULD_SHOW_SECURITY_WARNING = "should_show_security_warning";
        String FROM_SETTINGS = "from_settings";
        String API_V1_REQUEST_URL = "api_v1_request_url";
    }

    public static final String DAPP_HOMEPAGE_KEY = "dappHomePage";
    public static final String DAPP_LASTURL_KEY = "dappURL";
    public static final String DAPP_BROWSER_HISTORY = "DAPP_BROWSER_HISTORY";
    public static final String DAPP_BROWSER_BOOKMARKS = "dappBrowserBookmarks";
    public static final String DAPP_DEFAULT_URL = "https://www.stateofthedapps.com/";
    public static final String DAPP_PREFIX_TELEPHONE = "tel";
    public static final String DAPP_PREFIX_MAILTO = "mailto";
    public static final String DAPP_PREFIX_ALPHAWALLET = "alphawallet";
    public static final String DAPP_SUFFIX_RECEIVE = "receive";
    public static final String DAPP_PREFIX_MAPS = "maps.google.com/maps?daddr=";
    public static final String DAPP_PREFIX_WALLETCONNECT = "wc";
    public static final String DAPP_PREFIX_AWALLET = "awallet";

    public static final String ENS_SCAN_BLOCK = "ens_check_block";
    public static final String ENS_HISTORY = "ensHistory";
    public static final String ENS_HISTORY_PAIR = "ens_history_pair";


    public enum TokenStatus {
        DEFAULT, PENDING, INCOMPLETE;
    }

    public static final String INTERNET_SEARCH_PREFIX = "https://duckduckgo.com/?q=";
    public static final String HTTPS_PREFIX = "https://";

    // Settings Badge Keys
    public static final String KEY_NEEDS_BACKUP = "needsBackup";
    public static final String KEY_UPDATE_AVAILABLE = "updateAvailable";

    public static final String DEFAULT_CURRENCY_CODE = "USD";
    public static final String ACTION_MY_ADDRESS_SCREEN = "my_address_screen";

    //Analytics
    public static final String PREF_UNIQUE_ID = "unique_id";

    public static final String ALPHAWALLET_LOGO_URI = "https://alphawallet.com/wp-content/themes/alphawallet/img/logo-horizontal-new.svg";
    public static final String ALPHAWALLET_WEBSITE = "https://alphawallet.com";
    public static final String WALLET_CONNECT_REACT_APP_RELAY_URL = "wss://relay.walletconnect.com";
    public static final String ALPHA_WALLET_LOGO_URL = "https://user-images.githubusercontent.com/51817359/158344418-c0f2bd19-38bb-4e64-a1d5-25ceb099688a.png";

    // Theme/Dark Mode
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_AUTO = 2;

    // OpenSea APIs
    public static final String OPENSEA_COLLECTION_API_MAINNET = "https://api.opensea.io/collection/";
    public static final String OPENSEA_ASSETS_API_V2 = "https://api.opensea.io/api/v2/chain/{CHAIN}/account/{ADDRESS}/nfts";
    public static final String OPENSEA_NFT_API_V2 = "https://api.opensea.io/api/v2/chain/{CHAIN}/contract/{ADDRESS}/nfts/{TOKEN_ID}";

    //Timing
    public static long CONNECT_TIMEOUT = 10; //Seconds
    public static long READ_TIMEOUT = 10;
    public static long WRITE_TIMEOUT = 10;
    public static long PING_INTERVAL = 10;
    public static final long LONG_WRITE_TIMEOUT = 30;

    public static final String EXTERNAL_APP_DOWNLOAD_LINK = "https://alphawallet.com/download/AlphaWallet-release-build.apk";

    // shortcuts
    public static final String ACTION_TOKEN_SHORTCUT = "token_shortcut";
}
