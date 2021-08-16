package com.alphawallet.app.repository;

/* Please don't add import android at this point. Later this file will be shared
 * between projects including non-Android projects */

import android.text.TextUtils;

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
import com.google.gson.Gson;

import org.web3j.abi.datatypes.Address;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.http.HttpService;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.Single;

import static com.alphawallet.ethereum.EthereumNetworkBase.ARTIS_SIGMA1_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.ARTIS_TAU1_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.AVALANCHE_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.AVALANCHE_RPC_URL;
import static com.alphawallet.ethereum.EthereumNetworkBase.BINANCE_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.BINANCE_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.CLASSIC_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.CRONOS_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.FANTOM_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.FANTOM_RPC_URL;
import static com.alphawallet.ethereum.EthereumNetworkBase.FANTOM_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.FANTOM_TEST_RPC_URL;
import static com.alphawallet.ethereum.EthereumNetworkBase.FUJI_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.FUJI_TEST_RPC_URL;
import static com.alphawallet.ethereum.EthereumNetworkBase.GOERLI_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.HECO_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.HECO_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.KOVAN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MAINNET_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MATIC_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.MATIC_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.OPTIMISTIC_MAIN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.OPTIMISTIC_TEST_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.POA_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.RINKEBY_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.ROPSTEN_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.SOKOL_ID;
import static com.alphawallet.ethereum.EthereumNetworkBase.XDAI_ID;

public abstract class EthereumNetworkBase implements EthereumNetworkRepositoryType
{
    public static final String COVALENT = "[COVALENT]";

    private static final String DEFAULT_HOMEPAGE = "https://alphawallet.com/browser/";

    private static final String POLYGON_HOMEPAGE = "https://alphawallet.com/browser-item-category/polygon/";
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
    public static native String getSecondaryInfuraKey();
    public static native String getBSCExplorerKey();

    //Fallback nodes: these nodes are used if there's no Amberdata key, and also as a fallback in case the primary node times out while attempting a call
    public static final String MAINNET_RPC_URL = "https://mainnet.infura.io/v3/" + getInfuraKey();
    public static final String RINKEBY_RPC_URL = "https://rinkeby.infura.io/v3/" + getInfuraKey();

    //Note that AlphaWallet now uses a double node configuration. See class AWHttpService comment 'try primary node'.
    //If you supply a main RPC and secondary it will try the secondary if the primary node times out after 10 seconds.
    //See the declaration of NetworkInfo - it has a member backupNodeUrl. Put your secondary node here.

    public static final String BACKUP_INFURA_KEY = getSecondaryInfuraKey();
    public static final String MAINNET_FALLBACK_RPC_URL = "https://mainnet.infura.io/v3/" + getSecondaryInfuraKey();
    public static final String CLASSIC_RPC_URL = "https://www.ethercluster.com/etc";
    public static final String XDAI_RPC_URL = "https://rpc.xdaichain.com/";
    public static final String POA_RPC_URL = "https://core.poa.network/";
    public static final String ROPSTEN_RPC_URL = "https://ropsten.infura.io/v3/" + getInfuraKey();
    public static final String ROPSTEN_FALLBACK_RPC_URL = "https://ropsten.infura.io/v3/" + getSecondaryInfuraKey();
    public static final String RINKEBY_FALLBACK_RPC_URL = "https://rinkeby.infura.io/v3/" + getSecondaryInfuraKey();
    public static final String KOVAN_RPC_URL = "https://kovan.infura.io/v3/" + getInfuraKey();
    public static final String KOVAN_FALLBACK_RPC_URL = "https://kovan.infura.io/v3/" + getSecondaryInfuraKey();
    public static final String SOKOL_RPC_URL = "https://sokol.poa.network";
    public static final String GOERLI_RPC_URL = "https://goerli.infura.io/v3/" + getInfuraKey();
    public static final String GOERLI_FALLBACK_RPC_URL = "https://goerli.infura.io/v3/" + getSecondaryInfuraKey();
    public static final String ARTIS_SIGMA1_RPC_URL = "https://rpc.sigma1.artis.network";
    public static final String ARTIS_TAU1_RPC_URL = "https://rpc.tau1.artis.network";
    public static final String BINANCE_TEST_RPC_URL = "https://data-seed-prebsc-1-s3.binance.org:8545";
    public static final String BINANCE_TEST_FALLBACK_RPC_URL = "https://data-seed-prebsc-2-s1.binance.org:8545";
    public static final String BINANCE_MAIN_RPC_URL = "https://bsc-dataseed.binance.org";
    public static final String BINANCE_MAIN_FALLBACK_RPC_URL = "https://bsc-dataseed2.ninicoin.io:443";
    public static final String HECO_RPC_URL = "https://http-mainnet-node.huobichain.com";
    public static final String HECO_TEST_RPC_URL = "https://http-testnet.hecochain.com";
    public static final String OPTIMISTIC_MAIN_URL = "https://optimism-mainnet.infura.io/v3/" + getInfuraKey();
    public static final String OPTIMISTIC_TEST_URL = "https://optimism-kovan.infura.io/v3/" + getInfuraKey();
    public static final String MATIC_RPC_URL = "https://polygon-mainnet.infura.io/v3/" + getInfuraKey();
    public static final String MUMBAI_TEST_RPC_URL = "https://polygon-mumbai.infura.io/v3/" + getInfuraKey();
    public static final String MATIC_FALLBACK_RPC_URL = "https://matic-mainnet.chainstacklabs.com";
    public static final String MUMBAI_FALLBACK_RPC_URL = "https://matic-mumbai.chainstacklabs.com";
    public static final String OPTIMISTIC_MAIN_FALLBACK_URL = "https://mainnet.optimism.io";
    public static final String OPTIMISTIC_TEST_FALLBACK_URL = "https://kovan.optimism.io";
    public static final String CRONOS_TEST_URL = "https://cronos-testnet.crypto.org:8545";

    //This optional list creates a defined order in which tokens are displayed
    static final int[] orderList = {
            MAINNET_ID, CLASSIC_ID, XDAI_ID, POA_ID, ARTIS_SIGMA1_ID, KOVAN_ID, ROPSTEN_ID, SOKOL_ID,
            RINKEBY_ID, GOERLI_ID, ARTIS_TAU1_ID, BINANCE_TEST_ID, BINANCE_MAIN_ID, HECO_ID, HECO_TEST_ID,
            AVALANCHE_ID, FUJI_TEST_ID, FANTOM_ID, FANTOM_TEST_ID, MATIC_ID, MATIC_TEST_ID, OPTIMISTIC_MAIN_ID,
            OPTIMISTIC_TEST_ID//, CRONOS_TEST_ID
    };

    static final Map<Integer, NetworkInfo> networkMap = new HashMap<Integer, NetworkInfo>() {
        {
            put(MAINNET_ID, new NetworkInfo(C.ETHEREUM_NETWORK_NAME, C.ETH_SYMBOL,
                    MAINNET_RPC_URL,
                    "https://cn.etherscan.com/tx/", MAINNET_ID,
                    MAINNET_FALLBACK_RPC_URL, "https://api-cn.etherscan.com/api?"));
            put(CLASSIC_ID, new NetworkInfo(C.CLASSIC_NETWORK_NAME, C.ETC_SYMBOL,
                    CLASSIC_RPC_URL,
                    "https://blockscout.com/etc/mainnet/tx/", CLASSIC_ID, CLASSIC_RPC_URL,
                    "https://blockscout.com/etc/mainnet/api?"));
            put(XDAI_ID, new NetworkInfo(C.XDAI_NETWORK_NAME, C.xDAI_SYMBOL,
                    XDAI_RPC_URL,
                    "https://blockscout.com/xdai/mainnet/tx/", XDAI_ID,
                    "https://rpc.xdaichain.com", "https://blockscout.com/xdai/mainnet/api?"));
            put(POA_ID, new NetworkInfo(C.POA_NETWORK_NAME, C.POA_SYMBOL,
                    POA_RPC_URL,
                    "https://blockscout.com/poa/core/tx/", POA_ID, POA_RPC_URL,
                    "https://blockscout.com/poa/core/api?"));
            put(ARTIS_SIGMA1_ID, new NetworkInfo(C.ARTIS_SIGMA1_NETWORK, C.ARTIS_SIGMA1_SYMBOL,
                    ARTIS_SIGMA1_RPC_URL,
                    "https://explorer.sigma1.artis.network/tx/", ARTIS_SIGMA1_ID,
                    ARTIS_SIGMA1_RPC_URL, "https://explorer.sigma1.artis.network/api?"));
            put(KOVAN_ID, new NetworkInfo(C.KOVAN_NETWORK_NAME, C.ETH_SYMBOL,
                    KOVAN_RPC_URL,
                    "https://kovan.etherscan.io/tx/", KOVAN_ID,
                    KOVAN_FALLBACK_RPC_URL, "https://api-kovan.etherscan.io/api?"));
            put(ROPSTEN_ID, new NetworkInfo(C.ROPSTEN_NETWORK_NAME, C.ETH_SYMBOL,
                    ROPSTEN_RPC_URL,
                    "https://ropsten.etherscan.io/tx/", ROPSTEN_ID,
                    ROPSTEN_FALLBACK_RPC_URL, "https://api-ropsten.etherscan.io/api?"));
            put(SOKOL_ID, new NetworkInfo(C.SOKOL_NETWORK_NAME, C.POA_SYMBOL,
                    SOKOL_RPC_URL,
                    "https://blockscout.com/poa/sokol/tx/", SOKOL_ID,
                    SOKOL_RPC_URL, "https://blockscout.com/poa/sokol/api?"));
            put(RINKEBY_ID, new NetworkInfo(C.RINKEBY_NETWORK_NAME, C.ETH_SYMBOL,
                    RINKEBY_RPC_URL,
                    "https://rinkeby.etherscan.io/tx/", RINKEBY_ID,
                    RINKEBY_FALLBACK_RPC_URL, "https://api-rinkeby.etherscan.io/api?"));
            put(GOERLI_ID, new NetworkInfo(C.GOERLI_NETWORK_NAME, C.GOERLI_SYMBOL,
                    GOERLI_RPC_URL,
                    "https://goerli.etherscan.io/tx/", GOERLI_ID,
                    GOERLI_FALLBACK_RPC_URL, "https://api-goerli.etherscan.io/api?"));
            put(ARTIS_TAU1_ID, new NetworkInfo(C.ARTIS_TAU1_NETWORK, C.ARTIS_TAU1_SYMBOL,
                    ARTIS_TAU1_RPC_URL,
                    "https://explorer.tau1.artis.network/tx/", ARTIS_TAU1_ID,
                    ARTIS_TAU1_RPC_URL, "https://explorer.tau1.artis.network/api?"));
            put(BINANCE_TEST_ID, new NetworkInfo(C.BINANCE_TEST_NETWORK, C.BINANCE_SYMBOL,
                    BINANCE_TEST_RPC_URL,
                    "https://testnet.bscscan.com/tx/", BINANCE_TEST_ID,
                    BINANCE_TEST_FALLBACK_RPC_URL, "https://api-testnet.bscscan.com/api?"));
            put(BINANCE_MAIN_ID, new NetworkInfo(C.BINANCE_MAIN_NETWORK, C.BINANCE_SYMBOL,
                    BINANCE_MAIN_RPC_URL,
                    "https://bscscan.com/tx/", BINANCE_MAIN_ID,
                    BINANCE_MAIN_FALLBACK_RPC_URL, "https://api.bscscan.com/api?"));
            put(HECO_ID, new NetworkInfo(C.HECO_MAIN_NETWORK, C.HECO_SYMBOL,
                    HECO_RPC_URL,
                    "https://hecoinfo.com/tx/", HECO_ID,
                    HECO_RPC_URL, "https://api.hecoinfo.com/api?"));
            put(HECO_TEST_ID, new NetworkInfo(C.HECO_TEST_NETWORK, C.HECO_SYMBOL,
                    HECO_TEST_RPC_URL,
                    "https://testnet.hecoinfo.com/tx/", HECO_TEST_ID,
                    HECO_TEST_RPC_URL, "https://testnet.hecoinfo.com/api?"));
            put(AVALANCHE_ID, new NetworkInfo(C.AVALANCHE_NETWORK, C.AVALANCHE_SYMBOL,
                    AVALANCHE_RPC_URL,
                    "https://cchain.explorer.avax.network/tx/", AVALANCHE_ID,
                    AVALANCHE_RPC_URL, "https://api.covalenthq.com/v1/" + COVALENT));
            put(FUJI_TEST_ID, new NetworkInfo(C.FUJI_TEST_NETWORK, C.AVALANCHE_SYMBOL,
                    FUJI_TEST_RPC_URL,
                    "https://cchain.explorer.avax-test.network/tx/", FUJI_TEST_ID,
                    FUJI_TEST_RPC_URL, "https://api.covalenthq.com/v1/" + COVALENT));
            put(FANTOM_ID, new NetworkInfo(C.FANTOM_NETWORK, C.FANTOM_SYMBOL,
                    FANTOM_RPC_URL,
                    "https://ftmscan.com/tx/", FANTOM_ID,
                    FANTOM_RPC_URL, "https://api.ftmscan.com/api?"));
            put(FANTOM_TEST_ID, new NetworkInfo(C.FANTOM_TEST_NETWORK, C.FANTOM_SYMBOL,
                    FANTOM_TEST_RPC_URL,
                    "https://explorer.testnet.fantom.network/tx/", FANTOM_TEST_ID,
                    FANTOM_TEST_RPC_URL, "https://api.covalenthq.com/v1/" + COVALENT)); //NB: Fantom testnet not yet supported by Covalent
            put(MATIC_ID, new NetworkInfo(C.MATIC_NETWORK, C.MATIC_SYMBOL, MATIC_RPC_URL,
                    "https://polygonscan.com/tx/", MATIC_ID,
                    MATIC_FALLBACK_RPC_URL, "https://api.polygonscan.com/api?"));
            put(MATIC_TEST_ID, new NetworkInfo(C.MATIC_TEST_NETWORK, C.MATIC_SYMBOL,
                    MUMBAI_TEST_RPC_URL,
                    "https://mumbai.polygonscan.com/tx/", MATIC_TEST_ID,
                    MUMBAI_FALLBACK_RPC_URL, " https://api-testnet.polygonscan.com/api?"));
            put(OPTIMISTIC_MAIN_ID, new NetworkInfo(C.OPTIMISTIC_NETWORK, C.ETH_SYMBOL,
                    OPTIMISTIC_MAIN_URL,
                    "https://optimistic.etherscan.io/tx/", OPTIMISTIC_MAIN_ID, OPTIMISTIC_MAIN_FALLBACK_URL,
                    "https://api-optimistic.etherscan.io/api?"));
            put(OPTIMISTIC_TEST_ID, new NetworkInfo(C.OPTIMISTIC_TEST_NETWORK, C.ETH_SYMBOL,
                    OPTIMISTIC_TEST_URL,
                    "https://kovan-optimistic.etherscan.io/tx/", OPTIMISTIC_TEST_ID, OPTIMISTIC_TEST_FALLBACK_URL,
                    "https://api-kovan-optimistic.etherscan.io/api?"));
            /*put(CRONOS_TEST_ID, new NetworkInfo(C.CRONOS_TEST_NETWORK, C.CRONOS_SYMBOL,
                    CRONOS_TEST_URL,
                    "https://cronos-explorer.crypto.org/tx/", CRONOS_TEST_ID, CRONOS_TEST_URL,
                    "https://cronos-explorer.crypto.org/api?"));*/
        }
    };
    
    static final Map<Integer, String> addressOverride = new HashMap<Integer, String>() {
        {
            put(OPTIMISTIC_MAIN_ID, "0x4200000000000000000000000000000000000006");
            put(OPTIMISTIC_TEST_ID, "0x4200000000000000000000000000000000000006");
        }
    };

    final PreferenceRepositoryType preferences;
    private final Set<OnNetworkChangeListener> onNetworkChangedListeners = new HashSet<>();
    final boolean useTestNets;
    final NetworkInfo[] additionalNetworks;


    static class CustomNetworks {
        private ArrayList<NetworkInfo> list = new ArrayList<>();
        transient private Map<Integer, NetworkInfo> map = new HashMap<>();
        private Map<Integer, Boolean> mapToTestNet = new HashMap<>();
        transient private PreferenceRepositoryType preferences;

        public CustomNetworks(PreferenceRepositoryType preferences) {
            this.preferences = preferences;
            restore();
        }

        public void restore() {
            String networks = preferences.getCustomRPCNetworks();
            if (!TextUtils.isEmpty(networks)) {
                CustomNetworks cn = new Gson().fromJson(networks, CustomNetworks.class);
                this.list = cn.list;
                this.mapToTestNet = cn.mapToTestNet;

                for (NetworkInfo info : list) {
                    map.put(info.chainId, info);
                }
            }
        }

        public void addCustomNetwork(NetworkInfo info, boolean isTestnet) {
            list.add(info);
            mapToTestNet.put(info.chainId, isTestnet);
            map.put(info.chainId, info);
            String networks = new Gson().toJson(this);
            preferences.setCustomRPCNetworks(networks);
        }
    }

    private static CustomNetworks customNetworks;

    EthereumNetworkBase(PreferenceRepositoryType preferenceRepository, NetworkInfo[] additionalNetworks, boolean useTestNets)
    {
        this.preferences = preferenceRepository;
        this.additionalNetworks = additionalNetworks;
        this.useTestNets = useTestNets;

        this.customNetworks = new CustomNetworks(this.preferences);
    }

    private void addNetworks(NetworkInfo[] networks, List<NetworkInfo> result, boolean withValue)
    {
        for (NetworkInfo network : networks)
        {
            if (EthereumNetworkRepository.hasRealValue(network.chainId) == withValue) result.add(network);
        }
    }

    private void addNetworks(List<NetworkInfo> result, boolean withValue)
    {
        for (int networkId : orderList)
        {
            if (EthereumNetworkRepository.hasRealValue(networkId) == withValue) result.add(networkMap.get(networkId));
        }

        //Add in case no order was specified
        for (NetworkInfo info : networkMap.values())
        {
            if (EthereumNetworkRepository.hasRealValue(info.chainId) == withValue && !result.contains(info))
            {
                result.add(info);
            }
        }
    }

    public static String getChainOverrideAddress(int id) {
        return addressOverride.containsKey(id) ? addressOverride.get(id) : "";
    }

    @Override
    public String getNameById(int id)
    {
        NetworkInfo info = customNetworks.map.get(id);
        if (info != null) {
            return info.name;
        }

        if (networkMap.containsKey(id)) return networkMap.get(id).name;
        else return "Unknown: " + id;
    }

    @Override
    public NetworkInfo getActiveBrowserNetwork()
    {
        NetworkInfo info = customNetworks.map.get(preferences.getActiveBrowserNetwork());
        if (info != null) {
            return info;
        }

        return networkMap.get(preferences.getActiveBrowserNetwork());
    }

    @Override
    public NetworkInfo getNetworkByChain(int chainId)
    {
        NetworkInfo info = customNetworks.map.get(chainId);
        if (info != null) {
            return info;
        }
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
        return getSelectedFilters(preferences.isActiveMainnet());
    }

    @Override
    public List<Integer> getSelectedFilters(boolean isMainNet)
    {
        String filterList = preferences.getNetworkFilterList();
        List<Integer> storedIds = Utils.intListToArray(filterList);
        List<Integer> selectedIds = new ArrayList<>();

        for (Integer networkId : storedIds)
        {
            if (hasRealValue(networkId) == isMainNet) { selectedIds.add(networkId); }
        }

        if (selectedIds.size() == 0)
        {
            selectedIds.add(getDefaultNetwork(isMainNet));
            preferences.blankHasSetNetworkFilters();
            preferences.commit();
        }

        return selectedIds;
    }

    @Override
    public Integer getDefaultNetwork(boolean isMainNet)
    {
        return isMainNet ? MAINNET_ID : RINKEBY_ID;
    }

    @Override
    public void setFilterNetworkList(Integer[] networkList)
    {
        String store = Utils.intArrayToString(networkList);
        preferences.setNetworkFilterList(store.toString());
    }

    @Override
    public void setActiveBrowserNetwork(NetworkInfo networkInfo)
    {
        if (networkInfo != null)
        {
            preferences.setActiveBrowserNetwork(networkInfo.chainId);
            for (OnNetworkChangeListener listener : onNetworkChangedListeners)
            {
                listener.onNetworkChanged(networkInfo);
            }
        }
        else
        {
            preferences.setActiveBrowserNetwork(0);
        }
    }

    @Override
    public NetworkInfo[] getAvailableNetworkList()
    {
        //construct on demand, and give in order
        /* merging static compile time network list with runtime network list */
        List<NetworkInfo> networks = new ArrayList<>();

        /* the order is passed to the user interface. So if a user has a token on one
         * of the additionalNetworks, the same token on DEFAULT_NETWORKS, and on a few
         * test nets, they are displayed by that order.
         */
        addNetworks(customNetworks.list.toArray(new NetworkInfo[0]), networks, true);
        addNetworks(additionalNetworks, networks, true);
        addNetworks(networks, true);
        addNetworks(customNetworks.list.toArray(new NetworkInfo[0]), networks, false);
        addNetworks(additionalNetworks, networks, false);
        if (useTestNets) addNetworks(networks, false);
        return networks.toArray(new NetworkInfo[0]);
    }

    @Override
    public NetworkInfo[] getAllActiveNetworks()
    {
        List<NetworkInfo> networks = new ArrayList<>();
        addNetworks(networks, preferences.isActiveMainnet());
        return networks.toArray(new NetworkInfo[0]);
    }

    @Override
    public void addOnChangeDefaultNetwork(OnNetworkChangeListener onNetworkChanged) {
        onNetworkChangedListeners.add(onNetworkChanged);
    }

    public static boolean hasRealValue(int chainId)
    {
        switch (chainId)
        {
            case MAINNET_ID:
            case POA_ID:
            case CLASSIC_ID:
            case XDAI_ID:
            case ARTIS_SIGMA1_ID:
            case BINANCE_MAIN_ID:
            case HECO_ID:
            case AVALANCHE_ID:
            case FANTOM_ID:
            case MATIC_ID:
            case OPTIMISTIC_MAIN_ID:
                return true;
            default:
                if (customNetworks.mapToTestNet.containsKey(chainId)) {
                    return customNetworks.mapToTestNet.get(chainId) == false;
                }
                return false;
        }
    }

    public static String getSecondaryNodeURL(int networkId)
    {
        NetworkInfo info = networkMap.get(networkId);
        if (info == null) {
            info = customNetworks.map.get(networkId);
        }

        if (info != null) { return info.backupNodeUrl; }
        else {
            return "";
        }
    }

    //TODO: Fold this into file and add to database
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
            case FANTOM_ID:
                return R.drawable.ic_fantom;
            case FANTOM_TEST_ID:
                return R.drawable.ic_icons_fantom_test;
            case AVALANCHE_ID:
                return R.drawable.ic_icons_tokens_avalanche;
            case FUJI_TEST_ID:
                return R.drawable.ic_icons_tokens_avalanche_testnet;
            case MATIC_ID:
                return R.drawable.ic_icons_polygon;
            case MATIC_TEST_ID:
                return R.drawable.ic_icons_matic;
            case OPTIMISTIC_MAIN_ID:
                return R.drawable.ic_optimism_logo;
            case OPTIMISTIC_TEST_ID:
                return R.drawable.ic_optimism_testnet_logo;
            case CRONOS_TEST_ID:
                return R.drawable.ic_cronos;
            default:
                return R.drawable.ic_ethereum_logo;
        }
    }

    public static String getNodeURLByNetworkId(int networkId)
    {
        NetworkInfo info = networkMap.get(networkId);
        if (info == null) {
            info = customNetworks.map.get(networkId);
        }
        if (info != null) { return info.rpcServerUrl; }
        else { return MAINNET_RPC_URL; }
    }

    /**
     * This is used so as not to leak API credentials to web3; XInfuraAPI is the backup API key checked into github
     * @param networkId
     * @return
     */
    public static String getDefaultNodeURL(int networkId) {
        NetworkInfo info = networkMap.get(networkId);
        if (info == null) {
            info = customNetworks.map.get(networkId);
        }
        if (info != null) return info.rpcServerUrl;
        else return "";
    }

    public static String getEtherscanURLbyNetworkAndHash(int networkId, String txHash)
    {
        NetworkInfo info = networkMap.get(networkId);
        if (info == null) {
            info = customNetworks.map.get(networkId);
        }

        if (info != null)
        {
            return info.getEtherscanUri(txHash).toString();
        }
        else
        {
            return networkMap.get(MAINNET_ID).getEtherscanUri(txHash).toString();
        }
    }

    public static int getNetworkIdFromName(String name)
    {
        if (!TextUtils.isEmpty(name)) {
            for (NetworkInfo NETWORK : customNetworks.map.values()) {
                if (name.equals(NETWORK.name)) {
                    return NETWORK.chainId;
                }
            }
            for (NetworkInfo NETWORK : networkMap.values()) {
                if (name.equals(NETWORK.name)) {
                    return NETWORK.chainId;
                }
            }
        }
        return 0;
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
        return new ArrayList<>(Collections.singletonList(MAINNET_ID));
    }

    public static ContractLocator getOverrideToken()
    {
        return new ContractLocator("", MAINNET_ID, ContractType.ETHEREUM);
    }

    @Override
    public boolean isChainContract(int chainId, String address)
    {
        return (addressOverride.containsKey(chainId) && address.equalsIgnoreCase(addressOverride.get(chainId)));
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

    public static int decimalOverride(String address, int chainId)
    {
        return 0;
    }

    public static String defaultDapp(int chainId)
    {
        String dapp = (chainId == MATIC_ID || chainId == MATIC_TEST_ID) ? POLYGON_HOMEPAGE : DEFAULT_HOMEPAGE;
        return dapp;
    }

    public static boolean isDefaultDapp(String url)
    {
        return url != null && (url.equals(DEFAULT_HOMEPAGE)
                || url.equals(POLYGON_HOMEPAGE));
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

    public String getCurrentWalletAddress()
    {
        return preferences.getCurrentWalletAddress();
    }

    public boolean hasSetNetworkFilters()
    {
        return preferences.hasSetNetworkFilters();
    }

    public boolean isMainNetSelected()
    {
        return preferences.isActiveMainnet();
    }

    public void addCustomRPCNetwork(String networkName, String rpcUrl, int chainId, String symbol, String blockExplorerUrl, boolean isTestnet) {
        NetworkInfo info = new NetworkInfo(networkName, symbol, rpcUrl, blockExplorerUrl, chainId, null, null);
        customNetworks.addCustomNetwork(info, isTestnet);
    }
}
