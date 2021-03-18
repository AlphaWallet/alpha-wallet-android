package com.alphawallet.app.repository;

/* Please don't add import android at this point. Later this file will be shared
 * between projects including non-Android projects */

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractLocator;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenInfo;
import com.alphawallet.app.util.Utils;
import com.alphawallet.token.entity.ChainSpec;
import com.alphawallet.token.entity.MagicLinkInfo;

import org.web3j.abi.datatypes.Address;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.http.HttpService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.Single;

public abstract class EthereumNetworkBase implements EthereumNetworkRepositoryType
{
    private static final String DEFAULT_HOMEPAGE = "https://alphawallet.com/browser/";
    /* constructing URLs from BuildConfig. In the below area you will see hardcoded key like da3717...
       These hardcoded keys are fallbacks used by AlphaWallet forks.

       Also note: If you are running your own node and wish to use that; currently it must be hardcoded here
       If you wish your node to be the primary node that AW checks then replace the relevant ..._RPC_URL below
       If you wish your node to be the fallback, tried in case the primary times out then add/replace in ..._FALLBACK_RPC_URL list
     */

    static {
        System.loadLibrary("keys");
    }

    public static native String getAmberDataKey();
    public static native String getInfuraKey();

    //Fallback nodes: these nodes are used if there's no Amberdata key, and also as a fallback in case the primary node times out while attempting a call
    public static final String MAINNET_RPC_URL = "https://mainnet.infura.io/v3/" + getInfuraKey();
    public static final String RINKEBY_RPC_URL = "https://rinkeby.infura.io/v3/" + getInfuraKey();

    //Note that AlphaWallet now uses a double node configuration. See class AWHttpService comment 'try primary node'.
    //If you supply a main RPC and secondary it will try the secondary if the primary node times out after 10 seconds.
    //See the declaration of NetworkInfo - it has a member backupNodeUrl. Put your secondary node here.

    public static final String BACKUP_INFURA_KEY = BuildConfig.XInfuraAPI;
    public static final String MAINNET_FALLBACK_RPC_URL = !getAmberDataKey().startsWith("obtain") ? "https://rpc.web3api.io?x-api-key=" + getAmberDataKey() : MAINNET_RPC_URL;
    public static final String CLASSIC_RPC_URL = "https://www.ethercluster.com/etc";
    public static final String XDAI_RPC_URL = "https://dai.poa.network";
    public static final String POA_RPC_URL = "https://core.poa.network/";
    public static final String ROPSTEN_RPC_URL = "https://ropsten.infura.io/v3/" + getInfuraKey();
    public static final String RINKEBY_FALLBACK_RPC_URL = !getAmberDataKey().startsWith("obtain") ? "https://rpc.web3api.io?x-api-key=" + getAmberDataKey() + "&x-amberdata-blockchain-id=1b3f7a72b3e99c13" : RINKEBY_RPC_URL;
    public static final String KOVAN_RPC_URL = "https://kovan.infura.io/v3/" + getInfuraKey();
    public static final String SOKOL_RPC_URL = "https://sokol.poa.network";
    public static final String GOERLI_RPC_URL = "https://goerli.infura.io/v3/" + getInfuraKey();
    public static final String ARTIS_SIGMA1_RPC_URL = "https://rpc.sigma1.artis.network";
    public static final String ARTIS_TAU1_RPC_URL = "https://rpc.tau1.artis.network";
    public static final String BINANCE_TEST_RPC_URL = "https://data-seed-prebsc-1-s3.binance.org:8545";
    public static final String BINANCE_TEST_FALLBACK_RPC_URL = "https://data-seed-prebsc-2-s1.binance.org:8545";
    public static final String BINANCE_MAIN_RPC_URL = "https://bsc-dataseed1.binance.org:443";
    public static final String BINANCE_MAIN_FALLBACK_RPC_URL = "https://bsc-dataseed2.ninicoin.io:443";
    public static final String HECO_RPC_URL = "https://http-mainnet-node.huobichain.com";
    public static final String HECO_TEST_RPC_URL = "https://http-testnet.hecochain.com";

    public static final int MAINNET_ID = 1;
    public static final int CLASSIC_ID = 61;
    public static final int POA_ID = 99;
    public static final int KOVAN_ID = 42;
    public static final int ROPSTEN_ID = 3;
    public static final int SOKOL_ID = 77;
    public static final int RINKEBY_ID = 4;
    public static final int XDAI_ID = 100;
    public static final int HECO_ID = 128;
    public static final int HECO_TEST_ID = 256;
    public static final int GOERLI_ID = 5;
    public static final int ARTIS_SIGMA1_ID = 246529;
    public static final int ARTIS_TAU1_ID = 246785;
    public static final int BINANCE_TEST_ID = 97;
    public static final int BINANCE_MAIN_ID = 56;

    final Map<Integer, NetworkInfo> networkMap;

    final NetworkInfo[] NETWORKS;
    static final NetworkInfo[] DEFAULT_NETWORKS = new NetworkInfo[] {
            new NetworkInfo(C.ETHEREUM_NETWORK_NAME, C.ETH_SYMBOL,
                    MAINNET_RPC_URL,
                    "https://cn.etherscan.com/tx/",MAINNET_ID, true,
                    MAINNET_FALLBACK_RPC_URL,
                    "https://api-cn.etherscan.com/"),
            new NetworkInfo(C.CLASSIC_NETWORK_NAME, C.ETC_SYMBOL,
                    CLASSIC_RPC_URL,
                    "https://blockscout.com/etc/mainnet/tx/",CLASSIC_ID, true, CLASSIC_RPC_URL, "https://blockscout.com/etc/mainnet/"),
            new NetworkInfo(C.XDAI_NETWORK_NAME,
                    C.xDAI_SYMBOL,
                    XDAI_RPC_URL,
                    "https://blockscout.com/poa/dai/tx/",
                    XDAI_ID,
                    false,
                    "https://dai.poa.network",
                    "https://blockscout.com/poa/dai/"),
            new NetworkInfo(C.POA_NETWORK_NAME, C.POA_SYMBOL,
                    POA_RPC_URL,
                    "https://blockscout.com/poa/core/tx/", POA_ID, false, POA_RPC_URL, "https://blockscout.com/poa/core/"),
            new NetworkInfo(C.ARTIS_SIGMA1_NETWORK, C.ARTIS_SIGMA1_SYMBOL, ARTIS_SIGMA1_RPC_URL,
                    "https://explorer.sigma1.artis.network/tx/", ARTIS_SIGMA1_ID, false,
                    ARTIS_SIGMA1_RPC_URL,
                    "https://explorer.sigma1.artis.network/"),
            new NetworkInfo(C.KOVAN_NETWORK_NAME, C.ETH_SYMBOL, KOVAN_RPC_URL,
                    "https://kovan.etherscan.io/tx/", KOVAN_ID, false,
                    "https://kovan.infura.io/v3/" + BACKUP_INFURA_KEY,
                    "https://api-kovan.etherscan.io/"),
            new NetworkInfo(C.ROPSTEN_NETWORK_NAME, C.ETH_SYMBOL,
                    ROPSTEN_RPC_URL,
                    "https://ropsten.etherscan.io/tx/",ROPSTEN_ID, false,
                    "https://ropsten.infura.io/v3/" + BACKUP_INFURA_KEY,
                    "https://api-ropsten.etherscan.io/"),
            new NetworkInfo(C.SOKOL_NETWORK_NAME, C.POA_SYMBOL,
                    SOKOL_RPC_URL,
                    "https://blockscout.com/poa/sokol/tx/",SOKOL_ID, false, SOKOL_RPC_URL, "https://blockscout.com/poa/sokol/"),
            new NetworkInfo(C.RINKEBY_NETWORK_NAME, C.ETH_SYMBOL, RINKEBY_RPC_URL,
                    "https://rinkeby.etherscan.io/tx/",RINKEBY_ID, false,
                    RINKEBY_FALLBACK_RPC_URL,
                    "https://api-rinkeby.etherscan.io/"),
            new NetworkInfo(C.GOERLI_NETWORK_NAME, C.GOERLI_SYMBOL, GOERLI_RPC_URL,
                    "https://goerli.etherscan.io/tx/",GOERLI_ID, false,
                    GOERLI_RPC_URL,
                    "https://api-goerli.etherscan.io/"),
            new NetworkInfo(C.ARTIS_TAU1_NETWORK, C.ARTIS_TAU1_SYMBOL, ARTIS_TAU1_RPC_URL,
                    "https://explorer.tau1.artis.network/tx/", ARTIS_TAU1_ID, false,
                    ARTIS_TAU1_RPC_URL,
                    "https://explorer.tau1.artis.network/"),
            new NetworkInfo(C.BINANCE_TEST_NETWORK, C.BINANCE_SYMBOL, BINANCE_TEST_RPC_URL,
                    "https://testnet.bscscan.com/tx/", BINANCE_TEST_ID, false,
                    BINANCE_TEST_FALLBACK_RPC_URL,
                    "https://api-testnet.bscscan.com/"),
            new NetworkInfo(C.BINANCE_MAIN_NETWORK, C.BINANCE_SYMBOL, BINANCE_MAIN_RPC_URL,
                    "https://bscscan.com/tx/", BINANCE_MAIN_ID, false,
                    BINANCE_MAIN_FALLBACK_RPC_URL,
                    "https://api.bscscan.com/"),
            new NetworkInfo(C.HECO_MAIN_NETWORK, C.HECO_SYMBOL, HECO_RPC_URL,
                    "https://hecoinfo.com/tx/", HECO_ID, false,
                    HECO_RPC_URL,
                    "https://api.hecoinfo.com/"),
            new NetworkInfo(C.HECO_TEST_NETWORK, C.HECO_SYMBOL, HECO_TEST_RPC_URL,
                    "https://testnet.hecoinfo.com/tx/", HECO_TEST_ID, false,
                    HECO_TEST_RPC_URL,
                    "https://testnet.hecoinfo.com/"),
    };

    final PreferenceRepositoryType preferences;
    NetworkInfo defaultNetwork;
    private final Set<OnNetworkChangeListener> onNetworkChangedListeners = new HashSet<>();
    private boolean updatedTickers;

    EthereumNetworkBase(PreferenceRepositoryType preferenceRepository, NetworkInfo[] additionalNetworks, boolean useTestNets)
    {
        this.preferences = preferenceRepository;

        /* merging static compile time network list with runtime network list */
        List<NetworkInfo> networks = new ArrayList<>();

        /* the order is passed to the uesr interface. So if a user has a token on one
         * of the additionalNetworks, the same token on DEFAULT_NETWORKS, and on a few
         * test nets, they are displayed by that order.
         */
        addNetworks(additionalNetworks, networks, true);
        addNetworks(DEFAULT_NETWORKS, networks, true);
        addNetworks(additionalNetworks, networks, false);
        if (useTestNets) addNetworks(DEFAULT_NETWORKS, networks, false);

        /* then store the result list in a network variable */
        NETWORKS = networks.toArray(new NetworkInfo[0]);

        defaultNetwork = getByName(preferences.getDefaultNetwork());
        if (defaultNetwork == null) {
            defaultNetwork = NETWORKS[0];
        }

        networkMap = new ConcurrentHashMap<>();
        for (NetworkInfo network : NETWORKS)
        {
            networkMap.put(network.chainId, network);
        }

        updatedTickers = false;
    }

    private void addNetworks(NetworkInfo[] networks, List<NetworkInfo> result, boolean withValue)
    {
        for (NetworkInfo network : networks)
        {
            if (EthereumNetworkRepository.hasRealValue(network.chainId) == withValue) result.add(network);
        }
    }

    private NetworkInfo getByName(String name) {
        if (name != null && name != "") {
            for (NetworkInfo NETWORK : NETWORKS) {
                if (name.equals(NETWORK.name)) {
                    return NETWORK;
                }
            }
        }
        return null;
    }

    @Override
    public String getNameById(int id)
    {
        if (networkMap.containsKey(id)) return networkMap.get(id).name;
        else return "Unknown: " + id;
    }

    @Override
    public NetworkInfo getDefaultNetwork() {
        return defaultNetwork;
    }

    @Override
    public NetworkInfo getNetworkByChain(int chainId)
    {
        return networkMap.get(chainId);
    }

    // fetches the last transaction nonce; if it's identical to the last used one then increment by one
    // to ensure we don't get transaction replacement
    @Override
    public Single<BigInteger> getLastTransactionNonce(Web3j web3j, String walletAddress)
    {
        return Single.fromCallable(() -> {
            try
            {
                EthGetTransactionCount ethGetTransactionCount = web3j
                        .ethGetTransactionCount(walletAddress, DefaultBlockParameterName.PENDING)
                        .send();
                return ethGetTransactionCount.getTransactionCount();
            }
            catch (Exception e)
            {
                return BigInteger.ZERO;
            }
        });
    }

    @Override
    public List<Integer> getFilterNetworkList()
    {
        List<Integer> networkIds = EthereumNetworkRepository.addDefaultNetworks();
        String filterList = preferences.getNetworkFilterList();
        if (filterList.length() > 0)
        {
            networkIds = Utils.intListToArray(filterList);
        }

        return networkIds;
    }

    @Override
    public void setFilterNetworkList(int[] networkList)
    {
        String store = Utils.intArrayToString(networkList);
        preferences.setNetworkFilterList(store.toString());
    }

    @Override
    public void setDefaultNetworkInfo(NetworkInfo networkInfo) {
        defaultNetwork = networkInfo;
        preferences.setDefaultNetwork(defaultNetwork.name);

        for (OnNetworkChangeListener listener : onNetworkChangedListeners) {
            listener.onNetworkChanged(networkInfo);
        }
    }

    @Override
    public NetworkInfo[] getAvailableNetworkList() {
        return NETWORKS;
    }

    @Override
    public void addOnChangeDefaultNetwork(OnNetworkChangeListener onNetworkChanged) {
        onNetworkChangedListeners.add(onNetworkChanged);
    }

    public static boolean hasRealValue(int chainId)
    {
        switch (chainId)
        {
            case EthereumNetworkRepository.MAINNET_ID:
            case EthereumNetworkRepository.POA_ID:
            case EthereumNetworkRepository.CLASSIC_ID:
            case EthereumNetworkRepository.XDAI_ID:
            case EthereumNetworkRepository.ARTIS_SIGMA1_ID:
            case EthereumNetworkRepository.BINANCE_MAIN_ID:
            case EthereumNetworkRepository.HECO_ID:
                return true;

            default:
                return false;
        }
    }

    public static String getSecondaryNodeURL(int networkId) {
        switch (networkId)
        {
            case MAINNET_ID:
                return MAINNET_FALLBACK_RPC_URL;
            case KOVAN_ID:
                return KOVAN_RPC_URL;
            case ROPSTEN_ID:
                return ROPSTEN_RPC_URL;
            case RINKEBY_ID:
                return RINKEBY_FALLBACK_RPC_URL;
            case POA_ID:
                return POA_RPC_URL;
            case SOKOL_ID:
                return SOKOL_RPC_URL;
            case CLASSIC_ID:
                return CLASSIC_RPC_URL;
            case XDAI_ID:
                return XDAI_RPC_URL;
            case GOERLI_ID:
                return GOERLI_RPC_URL;
            case ARTIS_SIGMA1_ID:
                return ARTIS_SIGMA1_RPC_URL;
            case ARTIS_TAU1_ID:
                return ARTIS_TAU1_RPC_URL;
            case BINANCE_MAIN_ID:
                return BINANCE_MAIN_FALLBACK_RPC_URL;
            case BINANCE_TEST_ID:
                return BINANCE_TEST_FALLBACK_RPC_URL;
            case HECO_ID:
                return HECO_RPC_URL;
            case HECO_TEST_ID:
                return HECO_TEST_RPC_URL;
            default:
                return MAINNET_RPC_URL;
        }
    }

    public static int getChainLogo(int networkId) {
        switch (networkId)
        {
            case MAINNET_ID:
                return R.drawable.ic_token_eth;
            case KOVAN_ID:
                return R.drawable.kovan_logo;
            case ROPSTEN_ID:
                return R.drawable.ropsten_logo;
            case RINKEBY_ID:
                return R.drawable.rinkeby_logo;
            case POA_ID:
                return R.drawable.ic_poa_logo;
            case SOKOL_ID:
                return R.drawable.ic_poa_sokol;
            case CLASSIC_ID:
                return R.drawable.classic_logo;
            case XDAI_ID:
                return R.drawable.xdai_logo;
            case GOERLI_ID:
                return R.drawable.goerli_logo;
            case ARTIS_SIGMA1_ID:
                return R.drawable.ic_artis_sigma_logo;
            case ARTIS_TAU1_ID:
                return R.drawable.ic_artis_tau_logo;
            case BINANCE_MAIN_ID:
                return R.drawable.ic_binance_logo;
            case BINANCE_TEST_ID:
                return R.drawable.ic_binance_test_logo;
            case HECO_ID:
            case HECO_TEST_ID:
                return R.drawable.ic_heco_logo;
            default:
                return R.drawable.ic_ethereum_logo;
        }
    }

    public static String getNodeURLByNetworkId(int networkId) {
        switch (networkId)
        {
            case MAINNET_ID:
                return MAINNET_RPC_URL;
            case KOVAN_ID:
                return KOVAN_RPC_URL;
            case ROPSTEN_ID:
                return ROPSTEN_RPC_URL;
            case RINKEBY_ID:
                return RINKEBY_RPC_URL;
            case POA_ID:
                return POA_RPC_URL;
            case SOKOL_ID:
                return SOKOL_RPC_URL;
            case CLASSIC_ID:
                return CLASSIC_RPC_URL;
            case XDAI_ID:
                return XDAI_RPC_URL;
            case GOERLI_ID:
                return GOERLI_RPC_URL;
            case ARTIS_SIGMA1_ID:
                return ARTIS_SIGMA1_RPC_URL;
            case ARTIS_TAU1_ID:
                return ARTIS_TAU1_RPC_URL;
            case BINANCE_MAIN_ID:
                return BINANCE_MAIN_RPC_URL;
            case BINANCE_TEST_ID:
                return BINANCE_TEST_RPC_URL;
            case HECO_ID:
                return HECO_RPC_URL;
            case HECO_TEST_ID:
                return HECO_TEST_RPC_URL;
            default:
                return MAINNET_RPC_URL;
        }
    }

    /**
     * This is used so as not to leak API credentials to web3; XInfuraAPI is the backup API key checked into github
     * @param networkId
     * @return
     */
    public static String getDefaultNodeURL(int networkId) {
        switch (networkId)
        {
            case MAINNET_ID:
                return "https://mainnet.infura.io/v3/" + BuildConfig.XInfuraAPI;
            case KOVAN_ID:
                return "https://kovan.infura.io/v3/" + BuildConfig.XInfuraAPI;
            case ROPSTEN_ID:
                return "https://ropsten.infura.io/v3/" + BuildConfig.XInfuraAPI;
            case RINKEBY_ID:
                return "https://rinkeby.infura.io/v3/" + BuildConfig.XInfuraAPI;
            case GOERLI_ID:
                return "https://goerli.infura.io/v3/" + BuildConfig.XInfuraAPI;
            default:
                return getSecondaryNodeURL(networkId);
        }
    }

    public static String getMagicLinkDomainFromNetworkId(int networkId)
    {
        return MagicLinkInfo.getMagicLinkDomainFromNetworkId(networkId);
    }

    public static String getEtherscanURLbyNetwork(int networkId)
    {
        return MagicLinkInfo.getEtherscanURLbyNetwork(networkId);
    }

    public static boolean hasGasOverride(int chainId)
    {
        return false;
    }

    public static BigInteger gasOverrideValue(int chainId)
    {
        return BigInteger.valueOf(1);
    }

    public static List<ChainSpec> extraChains()
    {
        return null;
    }

    public static void addRequiredCredentials(int chainId, HttpService publicNodeService)
    {

    }

    public static List<Integer> addDefaultNetworks()
    {
        return new ArrayList<>(Collections.singletonList(EthereumNetworkRepository.MAINNET_ID));
    }

    public static ContractLocator getOverrideToken()
    {
        return new ContractLocator("", EthereumNetworkRepository.MAINNET_ID, ContractType.ETHEREUM);
    }

    public static boolean isPriorityToken(Token token)
    {
        return false;
    }

    public static int getPriorityOverride(Token token)
    {
        if (token.isEthereum()) return token.tokenInfo.chainId + 1;
        else return 0;
    }

    public static boolean showNetworkFilters() { return true; }

    public static int decimalOverride(String address, int chainId)
    {
        return 0;
    }

    public static String defaultDapp()
    {
        return DEFAULT_HOMEPAGE;
    }

    public Token getBlankOverrideToken(NetworkInfo networkInfo)
    {
        return createCurrencyToken(networkInfo);
    }

    public Single<Token[]> getBlankOverrideTokens(Wallet wallet)
    {
        return Single.fromCallable(() -> {
            if (getBlankOverrideToken() == null)
            {
                return new Token[0];
            }
            else
            {
                Token[] tokens = new Token[1];
                tokens[0] = getBlankOverrideToken();
                tokens[0].setTokenWallet(wallet.address);
                return tokens;
            }
        });
    }

    private static Token createCurrencyToken(NetworkInfo network)
    {
        TokenInfo tokenInfo = new TokenInfo(Address.DEFAULT.toString(), network.name, network.symbol, 18, true, network.chainId);
        BigDecimal balance = BigDecimal.ZERO;
        Token eth = new Token(tokenInfo, balance, 0, network.getShortName(), ContractType.ETHEREUM); //create with zero time index to ensure it's updated immediately
        eth.setTokenWallet(Address.DEFAULT.toString());
        eth.setIsEthereum();
        eth.pendingBalance = balance;
        return eth;
    }

    public Token getBlankOverrideToken()
    {
        return null;
    }
}
