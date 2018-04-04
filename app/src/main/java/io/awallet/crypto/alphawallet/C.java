package io.awallet.crypto.alphawallet;

public abstract class C {

    public static final int IMPORT_REQUEST_CODE = 1001;
    public static final int EXPORT_REQUEST_CODE = 1002;
    public static final int SHARE_REQUEST_CODE = 1003;

    public static final String ETHEREUM_NETWORK_NAME = "Ethereum";
    public static final String CLASSIC_NETWORK_NAME = "Ethereum Classic";
    public static final String POA_NETWORK_NAME = "POA Network";
    public static final String KOVAN_NETWORK_NAME = "Kovan (Test)";
    public static final String ROPSTEN_NETWORK_NAME = "Ropsten (Test)";
    public static final String SOKOL_NETWORK_NAME = "Sokol (Test)";

    public static final String ETHEREUM_TICKER = "ethereum";
    public static final String POA_TIKER = "poa";

    public static final String USD_SYMBOL = "$";
    public static final String ETH_SYMBOL = "ETH";
    public static final String POA_SYMBOL = "POA";
    public static final String ETC_SYMBOL = "ETC";

    public static final String GWEI_UNIT = "Gwei";

    public static final String MARKET_SALE = "market";
    public static final String MAGIC_LINK = "magic";

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
    public static final String STORMBIRD = "STORMBIRD";
    public static final String ERC875RANGE = "ERC875RANGE";
    public static final String TOKEN_TYPE = "TOKEN_TYPE";
    public static final String MARKET_INSTANCE = "MARKET_INSTANCE";
    public static final String IMPORT_STRING = "TOKEN_IMPORT";
    public static final String EXTRA_PRICE = "TOKEN_PRICE";

    public static final String COINBASE_WIDGET_CODE = "88d6141a-ff60-536c-841c-8f830adaacfd";
    public static final String SHAPESHIFT_KEY = "c4097b033e02163da6114fbbc1bf15155e759ddfd8352c88c55e7fef162e901a800e7eaecf836062a0c075b2b881054e0b9aa2324be7bc3694578493faf59af4";
    public static final String CHANGELLY_REF_ID = "968d4f0f0bf9";
    public static final String DONATION_ADDRESS = "0xFE6d4bC2De2D0b0E6FE47f08A28Ed52F9d052A02";

    public static final String DEFAULT_GAS_PRICE = "30000000000";
    public static final String DEFAULT_GAS_LIMIT = "90000";
    public static final String DEFAULT_GAS_LIMIT_FOR_TOKENS = "144000";
    public static final long GAS_LIMIT_MIN = 21000L;
    public static final long GAS_LIMIT_MAX = 300000L;
    public static final long GAS_PRICE_MIN = 1000000000L;
    public static final long NETWORK_FEE_MAX = 90000000000000000L;
    public static final int ETHER_DECIMALS = 18;

    //FOR DEMOS ETC
    public static final boolean SHOW_NEW_ACCOUNT_PROMPT = false;   //this will switch off the splash screen 'please make a key' message
    public static final boolean HARD_CODED_KEY = true;           //pre-loads a hard coded key in the app
    public static final boolean HARD_CODED_CONTRACT = false;      //pre-loads the contract as specified below
    public static final boolean OVERRIDE_DEFAULT_NETWORK = true;  //use the default network given below

    //Range of 3 test keys, they all have a little test eth pre-loaded on them
    public static final String HARD_PRIVATE_KEY1 = "bddb287b00c8047587f4fbf3de731a30f9404aa3735ae5d1766550534cf939da"; //0xc9034FF4266b1690d2B579584e5c3259009eD13c
    public static final String HARD_PRIVATE_KEY2 = "dcd6318be4fa8ab458b608804f06b8f25ccdab4f9d03c26fbbbe2b58e42f4df5"; //0x97e2bde4654Ca8Ea2cC30335Ea85eC1F9b10604A
    public static final String HARD_PRIVATE_KEY3 = "fa41ce4f689c883584cfcf29cd2526c7f4de4dede260753b0234e475b216a733"; //0x93922cDaBAa26d50E7C6Cb19EE3bCd03462Ed334

    //Pick which private key to pre-install
    public static final String PRE_LOADED_KEY = HARD_PRIVATE_KEY3;
    public static final String HARD_CONTRACT_ADDR = "0x0B6732BAECC0793E38A98934799ABD3C7DC3CF31";
    //TODO: Pick these up from blockchain
    public static final String HARD_CONTRACT_NAME = "World Series Baseball";
    public static final String HARD_CONTRACT_SYMBOL = "WSB";

    public static final String DEFAULT_NETWORK = ROPSTEN_NETWORK_NAME;

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
    }
}
