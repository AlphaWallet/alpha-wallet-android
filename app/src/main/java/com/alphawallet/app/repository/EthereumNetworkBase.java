package com.alphawallet.app.repository;

import android.content.Context;
import android.text.TextUtils;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.ContractResult;
import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Ticker;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.TokenTicker;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.util.Utils;
import com.alphawallet.token.entity.ChainSpec;
import com.alphawallet.token.entity.MagicLinkInfo;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.http.HttpService;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public abstract class EthereumNetworkBase implements EthereumNetworkRepositoryType
{
    public static final String MAINNET_RPC_URL = "https://mainnet.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    public static final String CLASSIC_RPC_URL = "https://ethereumclassic.network";
    public static final String XDAI_RPC_URL = "https://dai.poa.network";
    public static final String POA_RPC_URL = "https://core.poa.network/";
    public static final String ROPSTEN_RPC_URL = "https://ropsten.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    public static final String RINKEBY_RPC_URL = "https://rinkeby.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    public static final String KOVAN_RPC_URL = "https://kovan.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    public static final String SOKOL_RPC_URL = "https://sokol.poa.network";
    public static final String GOERLI_RPC_URL = "https://goerli.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
    public static final String ARTIS_SIGMA1_RPC_URL = "https://rpc.sigma1.artis.network";
    public static final String ARTIS_TAU1_RPC_URL = "https://rpc.tau1.artis.network";

    public static final int MAINNET_ID = 1;
    public static final int CLASSIC_ID = 61;
    public static final int POA_ID = 99;
    public static final int KOVAN_ID = 42;
    public static final int ROPSTEN_ID = 3;
    public static final int SOKOL_ID = 77;
    public static final int RINKEBY_ID = 4;
    public static final int XDAI_ID = 100;
    public static final int GOERLI_ID = 5;
    public static final int ARTIS_SIGMA1_ID = 246529;
    public static final int ARTIS_TAU1_ID = 246785;

    public static final String BLOCKSCOUT_API = "https://blockscout.com/";
    public static final String BLOCKSCOUT_TOKEN_ARGS = "/api?module=account&action=tokenlist&address=";

    public static final String MAINNET_BLOCKSCOUT = "eth/mainnet";
    public static final String CLASSIC_BLOCKSCOUT = "etc/mainnet";
    public static final String XDAI_BLOCKSCOUT = "poa/dai";
    public static final String POA_BLOCKSCOUT = "poa/core";
    public static final String ROPSTEN_BLOCKSCOUT = "eth/ropsten";
    public static final String RINKEBY_BLOCKSCOUT = "eth/rinkeby";
    public static final String SOKOL_BLOCKSCOUT = "poa/sokol";
    public static final String KOVAN_BLOCKSCOUT = "eth/kovan";
    public static final String GOERLI_BLOCKSCOUT = "eth/goerli";

    private final Map<Integer, NetworkInfo> networkMap;

    private final NetworkInfo[] NETWORKS;
    private NetworkInfo[] DEFAULT_NETWORKS = new NetworkInfo[] {
            new NetworkInfo(C.ETHEREUM_NETWORK_NAME, C.ETH_SYMBOL,
                    MAINNET_RPC_URL,
                    "https://etherscan.io/tx/",MAINNET_ID, true,
                    "https://mainnet.infura.io/v3/da3717f25f824cc1baa32d812386d93f",
                    "https://api.etherscan.io/",
                    C.ETHEREUM_TICKER_NAME,
                    MAINNET_BLOCKSCOUT),
            new NetworkInfo(C.CLASSIC_NETWORK_NAME, C.ETC_SYMBOL,
                    CLASSIC_RPC_URL,
                    "https://gastracker.io/tx/",CLASSIC_ID, true, C.CLASSIC_TICKER_NAME, CLASSIC_BLOCKSCOUT),
            new NetworkInfo(C.XDAI_NETWORK_NAME,
                    C.xDAI_SYMBOL,
                    XDAI_RPC_URL,
                    "https://blockscout.com/poa/dai/tx/",
                    XDAI_ID,
                    false,
                    "https://dai.poa.network",
                    "https://blockscout.com/poa/dai/", C.XDAI_TICKER_NAME, XDAI_BLOCKSCOUT),
            new NetworkInfo(C.POA_NETWORK_NAME, C.POA_SYMBOL,
                    POA_RPC_URL,
                    "https://poaexplorer.com/txid/search/", POA_ID, false, C.ETHEREUM_TICKER_NAME, POA_BLOCKSCOUT),
            new NetworkInfo(C.ARTIS_SIGMA1_NETWORK, C.ARTIS_SIGMA1_SYMBOL, ARTIS_SIGMA1_RPC_URL,
                    "https://explorer.sigma1.artis.network/tx/", ARTIS_SIGMA1_ID, false,
                    ARTIS_SIGMA1_RPC_URL,
                    "https://explorer.sigma1.artis.network/", C.ARTIS_SIGMA_TICKER, ""),
            new NetworkInfo(C.KOVAN_NETWORK_NAME, C.ETH_SYMBOL, KOVAN_RPC_URL,
                    "https://kovan.etherscan.io/tx/", KOVAN_ID, false,
                    "https://kovan.infura.io/v3/da3717f25f824cc1baa32d812386d93f",
                    "https://api-kovan.etherscan.io/", C.ETHEREUM_TICKER_NAME, KOVAN_BLOCKSCOUT),
            new NetworkInfo(C.ROPSTEN_NETWORK_NAME, C.ETH_SYMBOL,
                    ROPSTEN_RPC_URL,
                    "https://ropsten.etherscan.io/tx/",ROPSTEN_ID, false,
                    "https://ropsten.infura.io/v3/da3717f25f824cc1baa32d812386d93f",
                    "https://api-ropsten.etherscan.io/", C.ETHEREUM_TICKER_NAME, ROPSTEN_BLOCKSCOUT),
            new NetworkInfo(C.SOKOL_NETWORK_NAME, C.POA_SYMBOL,
                    SOKOL_RPC_URL,
                    "https://sokol-explorer.poa.network/account/",SOKOL_ID, false, C.ETHEREUM_TICKER_NAME, SOKOL_BLOCKSCOUT),
            new NetworkInfo(C.RINKEBY_NETWORK_NAME, C.ETH_SYMBOL, RINKEBY_RPC_URL,
                    "https://rinkeby.etherscan.io/tx/",RINKEBY_ID, false,
                    "https://rinkeby.infura.io/v3/da3717f25f824cc1baa32d812386d93f",
                    "https://api-rinkeby.etherscan.io/", C.ETHEREUM_TICKER_NAME, RINKEBY_BLOCKSCOUT),
            new NetworkInfo(C.GOERLI_NETWORK_NAME, C.GOERLI_SYMBOL, GOERLI_RPC_URL,
                    "https://goerli.etherscan.io/tx/",GOERLI_ID, false,
                    GOERLI_RPC_URL,
                    "https://api-goerli.etherscan.io/", C.ETHEREUM_TICKER_NAME, GOERLI_BLOCKSCOUT),
            new NetworkInfo(C.ARTIS_TAU1_NETWORK, C.ARTIS_TAU1_SYMBOL, ARTIS_TAU1_RPC_URL,
                    "https://explorer.tau1.artis.network/tx/", ARTIS_TAU1_ID, false,
                    ARTIS_TAU1_RPC_URL,
                    "https://explorer.tau1.artis.network/", C.ARTIS_SIGMA_TICKER, ""),
    };

    private final PreferenceRepositoryType preferences;
    private final TickerService tickerService;
    private NetworkInfo defaultNetwork;
    private final Set<OnNetworkChangeListener> onNetworkChangedListeners = new HashSet<>();
    private Map<Integer, Ticker> ethTickers = new ConcurrentHashMap<>();
    private Map<Integer, Long> ethTickerTimes = new ConcurrentHashMap<>();
    private boolean updatedTickers;

    public EthereumNetworkBase(PreferenceRepositoryType preferenceRepository, TickerService tickerService, NetworkInfo[] additionalNetworks)
    {
        this.preferences = preferenceRepository;
        this.tickerService = tickerService;
        NETWORKS = (NetworkInfo[]) Array.newInstance(NetworkInfo.class, additionalNetworks.length + DEFAULT_NETWORKS.length);
        System.arraycopy(additionalNetworks, 0, NETWORKS, 0, additionalNetworks.length);
        System.arraycopy(DEFAULT_NETWORKS, 0, NETWORKS, additionalNetworks.length, DEFAULT_NETWORKS.length);

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

        this.tickerService.fetchAmberData()
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.newThread())
                .subscribe(this::updateTickers).isDisposed();
    }

    private NetworkInfo getByName(String name) {
        if (!TextUtils.isEmpty(name)) {
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
            EthGetTransactionCount ethGetTransactionCount = web3j
                    .ethGetTransactionCount(walletAddress, DefaultBlockParameterName.PENDING)
                    .send();
            return ethGetTransactionCount.getTransactionCount();
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

    @Override
    public Single<Ticker> getTicker(int chainId, TokenTicker tTicker) {
        if (tTicker != null && !tTicker.percentChange24h.equals("0.00") && !tTicker.percentChange24h.equals("0")) return Single.fromCallable(() -> {
            Ticker ticker = new Ticker();
            ticker.id = tTicker.id;
            ticker.price_usd = tTicker.price;
            ticker.percentChange24h = tTicker.percentChange24h;
            ticker.symbol = getNetworkByChain(chainId).symbol;
            ethTickers.put(chainId, ticker);
            return ticker;
        });

        NetworkInfo network = networkMap.get(chainId);
        switch (network.tickerId)
        {
            case C.ARTIS_SIGMA_TICKER:
                return tickerService.convertPair("EUR", "USD")
                        .map(this::getSigmaTicker);
            default:
                return updateTicker(network);
        }
    }

    private Single<Ticker> updateTicker(NetworkInfo networkInfo)
    {
        Ticker ticker = ethTickers.get(networkInfo.chainId);
        if (ticker != null)
        {
            if (!ticker.price_usd.equals("0.00") && ethTickerTimes.containsKey(networkInfo.chainId))
            {
                long lastTry = ethTickerTimes.get(networkInfo.chainId);
                if (System.currentTimeMillis() - lastTry < 5000 * 60) return Single.fromCallable(() -> ticker); //reduce network traffic
            }

            //just update the price
            switch (networkInfo.chainId)
            {
                case CLASSIC_ID:
                case POA_ID:
                case SOKOL_ID:
                case XDAI_ID:
                    return tickerService.fetchBlockScoutPrice(networkInfo, ticker)
                            .map(newTicker -> updateTicker(networkInfo.chainId, newTicker));
                case MAINNET_ID:
                    return tickerService.fetchEthPrice(networkInfo, ticker)
                            .map(newTicker -> updateTicker(networkInfo.chainId, newTicker));
                default:
                    return Single.fromCallable(() -> ethTickers.get(networkInfo.chainId));
            }
        }
        else
        {
            switch (networkInfo.chainId)
            {
                case MAINNET_ID:
                    return blankTicker(networkInfo);
                case XDAI_ID:
                    return tickerService.fetchBlockScoutPrice(networkInfo, ticker);
                case CLASSIC_ID:
                default:
                    return blankTicker(networkInfo);
                case POA_ID:
                case SOKOL_ID:
                    //we have POA, use CoinMarketCap
                    //first stop successive update
                    updateTicker(networkInfo.chainId, getBlankTicker(networkInfo));
                    return tickerService.fetchCMCTickers()
                            .map(this::updateCMCTickers);
            }
        }
    }

    private Ticker getSigmaTicker(double rate)
    {
        Ticker artisTicker = new Ticker();
        artisTicker.percentChange24h = "0.00";
        double conversion = (1.0/13.7603)*rate; //13.7603 ATS = 1 EUR
        artisTicker.price_usd = String.valueOf(conversion);
        artisTicker.symbol = "USD";
        return artisTicker;
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
                return true;

            default:
                return false;
        }
    }

    public static String getNodeURLByNetworkId(int networkId) {
        return MagicLinkInfo.getNodeURLByNetworkId(networkId);
    }

    public static String getMagicLinkDomainFromNetworkId(int networkId)
    {
        return MagicLinkInfo.getMagicLinkDomainFromNetworkId(networkId);
    }

    public static String getEtherscanURLbyNetwork(int networkId)
    {
        return MagicLinkInfo.getEtherscanURLbyNetwork(networkId);
    }

    protected static List<ContractResult> getAllKnownContracts(Context context, List<Integer> chainFilters)
    {
        List<ContractResult> knownContracts = new ArrayList<>(getAllKnownContractsOnNetwork(context, EthereumNetworkRepository.MAINNET_ID, chainFilters));
        knownContracts.addAll(getAllKnownContractsOnNetwork(context, EthereumNetworkRepository.XDAI_ID, chainFilters));
        return knownContracts;
    }

    protected static List<ContractResult> getAllKnownContractsOnNetwork(Context context, int chainId, List<Integer> filters)
    {
        int index = 0;

        if (!filters.contains((Integer)chainId)) return new ArrayList<>();

        List<ContractResult> result = new ArrayList<>();
        switch (chainId)
        {
            case EthereumNetworkRepository.XDAI_ID:
                index = R.array.xDAI;
                break;
            case EthereumNetworkRepository.MAINNET_ID:
                index = R.array.MainNet;
                break;
            default:
                break;
        }

        if (index > 0)
        {
            String[] strArray = context.getResources().getStringArray(index);
            for (String addr : strArray)
            {
                result.add(new ContractResult(addr, chainId));
            }
        }

        return result;
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
        return new ArrayList<>(EthereumNetworkRepository.MAINNET_ID);
    }

    public static boolean hasTicker(Token token)
    {
        return (token.ticker != null || token.isEthereum());
    }
    public static ContractResult getOverrideToken()
    {
        return new ContractResult("", EthereumNetworkRepository.MAINNET_ID, ContractType.ETHEREUM);
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


    private Ticker updateTickers(Map<Integer, Ticker> newTickers)
    {
        for (Integer chainId : newTickers.keySet())
        {
            Ticker ticker = newTickers.get(chainId);
            ethTickers.put(chainId, ticker);
            ethTickerTimes.put(chainId, System.currentTimeMillis());
        }

        if (newTickers.size() == 0)
        {
            for (NetworkInfo network : NETWORKS)
            {
                ethTickers.put(network.chainId, getBlankTicker(network));
            }
        }

        return ethTickers.get(MAINNET_ID);
    }

    private Ticker updateCMCTickers(Map<Integer, Ticker> newTickers)
    {
        for (Integer chainId : newTickers.keySet())
        {
            Ticker ticker = newTickers.get(chainId);
            Ticker oTicker = ethTickers.get(chainId);
            if (oTicker == null || oTicker.percentChange24h.equals("0.00") || oTicker.percentChange24h.equals("0"))
            {
                ethTickers.put(chainId, ticker);
                ethTickerTimes.put(chainId, System.currentTimeMillis());
            }
        }

        if (ethTickers.get(POA_ID) != null) return ethTickers.get(POA_ID);
        else return getBlankTicker(networkMap.get(POA_ID));
    }

    private Single<Ticker> blankTicker(NetworkInfo networkInfo)
    {
        return Single.fromCallable(() -> getBlankTicker(networkInfo));
    }

    private Ticker getBlankTicker(NetworkInfo networkInfo)
    {
        Ticker tempTicker = new Ticker();
        tempTicker.id = String.valueOf(networkInfo.chainId);
        tempTicker.percentChange24h = "0";
        tempTicker.price_usd = "0.00";
        return tempTicker;
    }

    private Ticker updateTicker(int chainId, Ticker newTicker)
    {
        ethTickers.put(chainId, newTicker);
        ethTickerTimes.put(chainId, System.currentTimeMillis());
        switch (chainId)
        {
            case MAINNET_ID:
                ethTickers.put(chainId, newTicker);
                ethTickers.put(GOERLI_ID, newTicker);
                ethTickers.put(RINKEBY_ID, newTicker);
                ethTickers.put(ROPSTEN_ID, newTicker);
                ethTickers.put(KOVAN_ID, newTicker);
                break;
            case POA_ID:
                ethTickers.put(SOKOL_ID, newTicker);
                break;
            default:
                break;
        }
        return newTicker;
    }

    @Override
    public Single<Token> attachTokenTicker(Token token)
    {
        return tickerService.attachTokenTicker(token);
    }

    @Override
    public Single<Token[]> attachTokenTickers(Token[] tokens)
    {
        updatedTickers = true;
        return tickerService.attachTokenTickers(tokens);
    }

    @Override
    public TokenTicker getTokenTicker(Token token)
    {
        return tickerService.getTokenTicker(token);
    }

    @Override
    public boolean checkTickers()
    {
        return !updatedTickers && tickerService.hasTickers();
    }
}
