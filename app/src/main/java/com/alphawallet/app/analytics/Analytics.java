package com.alphawallet.app.analytics;

public class Analytics
{
    public static final String ERROR = "Error";

    // Properties
    public static final String PROPS_IMPORT_WALLET_TYPE = "import_wallet_type";
    public static final String PROPS_WALLET_TYPE = "wallet_type";
    public static final String PROPS_GAS_SPEED = "gas_speed";
    public static final String PROPS_FROM_WALLET_CONNECT = "from_wallet_connect";
    public static final String PROPS_URL = "url";
    public static final String PROPS_QR_SCAN_SOURCE = "qr_scan_source";
    public static final String PROPS_ACTION_SHEET_SOURCE = "action_sheet_source";
    public static final String PROPS_ACTION_SHEET_MODE = "action_sheet_mode";
    public static final String PROPS_SWAP_FROM_TOKEN = "from_token";
    public static final String PROPS_SWAP_TO_TOKEN = "to_token";
    public static final String PROPS_ERROR_MESSAGE = "error_message";

    public enum Navigation
    {
        WALLET("Screen: Wallet"),
        ACTIVITY("Screen: Activity"),
        BROWSER("Screen: Browser"),
        SETTINGS("Screen: Settings"),
        IMPORT_WALLET("Screen: Import Wallet"),
        COINBASE_PAY("Screen: Buy with Coinbase Pay"),
        WALLET_CONNECT("Screen: Wallet Connect"),

        ACTION_SHEET_CONFIRMATION("Screen: Txn Confirmation"),
        ACTION_SHEET_FOR_TRANSACTION_CONFIRMATION("Screen: Txn Confirmation"),
        ACTION_SHEET_FOR_TRANSACTION_CONFIRMATION_SUCCESSFUL("Screen: Txn Confirmation Successful"),
        ACTION_SHEET_FOR_TRANSACTION_CONFIRMATION_FAILED("Screen: Txn Confirmation Failed"),
        TOKEN_SWAP("Screen: Token Swap"),

        SCAN_QR_CODE("Screen: QR Code Scanner");
//        ON_RAMP("Screen: Fiat On-Ramp"),
//        ON_UNISWAP("Screen: Uniswap"),
//        ON_XDAI_BRIDGE("Screen: xDai Bridge"),
//        ON_HONEYSWAP("Screen: Honeyswap"),
//        ON_ONEINCH("Screen: Oneinch"),
//        ON_CARTHAGE("Screen: Carthage"),
//        ON_ARBITRUM_BRIDGE("Screen: Arbitrum Bridge"),
//        ON_QUICK_SWAP("Screen: QuickSwap"),
//        FALLBACK("Screen: <Fallback>"),
//        SWITCH_SERVERS("Screen: Switch Servers"),
//        SHOW_DAPPS("Screen: Dapps"),
//        SHOW_HISTORY("Screen: Dapp History"),
//        TAP_BROWSER_MORE("Screen: Browser More Options"),
//        SIGN_MESSAGE_REQUEST("Screen: Sign Message Request"),
//        WALLET_CONNECT("Screen: WalletConnect"),
//        DEEP_LINK("Screen: DeepLink"),
//        FAQ("Screen: FAQ"),
//        DISCORD("Screen: Discord"),
//        TELEGRAM_CUSTOMER_SUPPORT("Screen: Telegram: Customer Support"),
//        TWITTER("Screen: Twitter"),
//        REDDIT("Screen: Reddit"),
//        FACEBOOK("Screen: Facebook"),
//        GITHUB("Screen: Github"),
//        EXPLORER("Screen: Explorer"),
//        OPEN_SHORTCUT("Screen: Shortcut"),
//        OPEN_HELP_URL("Screen: Help URL"),
//        BLOCKSCAN_CHAT("Screen: Blockscan Chat");

        private final String screenName;

        Navigation(String screenName)
        {
            this.screenName = screenName;
        }

        public String getValue()
        {
            return screenName;
        }
    }

    public enum Action
    {
        IMPORT_WALLET("Import Wallet"),
        USE_GAS_WIDGET("Use Gas Widget"),
        LOAD_URL("Load URL"),
        ACTION_SHEET_COMPLETED("ActionSheet Completed"),
        ACTION_SHEET_CANCELLED("ActionSheet Cancelled"),
        SCAN_QR_CODE_SUCCESS("Scan QR Code Completed"),
        SCAN_QR_CODE_CANCELLED("Scan QR Code Cancelled"),
        SCAN_QR_CODE_ERROR("Scan QR Code Error");
//        CANCELS_TRANSACTION_IN_ACTION_SHEET("Txn Confirmation Cancelled"),
//        RELOAD_BROWSER("Reload Browser"),
//        SHARE_URL("Share URL"),
//        ADD_DAPP("Add Dapp"),
//        ENTER_URL("Enter URL"),
//        SIGN_MESSAGE_REQUEST("Sign Message Request"),
//        CANCEL_SIGN_MESSAGE_REQUEST("Cancel Sign Message Request"),
//        SWITCHED_SERVER("Switch Server Completed"),
//        CANCELS_SWITCH_SERVER("Switch Server Cancelled"),
//        WALLET_CONNECT_CONNECT("WalletConnect Connect"),
//        WALLET_CONNECT_CANCEL("WalletConnect Cancel"),
//        WALLET_CONNECT_DISCONNECT("WalletConnect Disconnect"),
//        WALLET_CONNECT_SWITCH_NETWORK("WalletConnect Switch Network"),
//        WALLET_CONNECT_CONNECTION_TIMEOUT("WalletConnect Connection Timeout"),
//        WALLET_CONNECT_CONNECTION_FAILED("WalletConnect Connection Failed"),
//        CLEAR_BROWSER_CACHE("Clear Browser Cache"),
//        PING_INFURA("Ping Infura"),
//        RECTIFY_SEND_TRANSACTION_ERROR_IN_ACTION_SHEET("Rectify Send Txn Error"),
//        NAME_WALLET("Name Wallet"),
//        FIRST_WALLET_ACTION("First Wallet Action"),
//        ADD_CUSTOM_CHAIN("Add Custom Chain"),
//        EDIT_CUSTOM_CHAIN("Edit Custom Chain"),
//        SUBSCRIBE_TO_EMAIL_NEWSLETTER("Subscribe Email Newsletter"),
//        TAP_SAFARI_EXTENSION_REWRITTEN_URL("Tap Safari Extension Rewritten URL");

        private final String actionName;

        Action(String actionName)
        {
            this.actionName = "Action: " + actionName;
        }

        public String getValue()
        {
            return actionName;
        }
        }
}
