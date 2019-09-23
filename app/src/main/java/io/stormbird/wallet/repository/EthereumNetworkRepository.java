package io.stormbird.wallet.repository;

import android.text.TextUtils;

import io.stormbird.wallet.util.Utils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;

import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Ticker;
import io.stormbird.wallet.service.TickerService;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.Single;

import static io.stormbird.wallet.C.*;

public class EthereumNetworkRepository implements EthereumNetworkRepositoryType {

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

	private static final String MAINNET_BLOCKSCOUT = "eth/mainnet";
	private static final String CLASSIC_BLOCKSCOUT = "etc/mainnet";
	private static final String XDAI_BLOCKSCOUT = "poa/dai";
	private static final String POA_BLOCKSCOUT = "poa/core";
	private static final String ROPSTEN_BLOCKSCOUT = "eth/ropsten";
	private static final String RINKEBY_BLOCKSCOUT = "eth/rinkeby";
	private static final String SOKOL_BLOCKSCOUT = "poa/sokol";
	private static final String KOVAN_BLOCKSCOUT = "eth/kovan";
	private static final String GOERLI_BLOCKSCOUT = "eth/goerli";

	private final Map<Integer, NetworkInfo> networkMap;

	private final NetworkInfo[] NETWORKS = new NetworkInfo[] {
			new NetworkInfo(ETHEREUM_NETWORK_NAME, ETH_SYMBOL,
                    MAINNET_RPC_URL,
                    "https://etherscan.io/tx/",MAINNET_ID, true,
							"https://mainnet.infura.io/v3/da3717f25f824cc1baa32d812386d93f",
							"https://api.etherscan.io/",
							ETHEREUM_TICKER_NAME,
							MAINNET_BLOCKSCOUT),
      new NetworkInfo(CLASSIC_NETWORK_NAME, ETC_SYMBOL,
                    CLASSIC_RPC_URL,
					  "https://gastracker.io/tx/",CLASSIC_ID, true, CLASSIC_TICKER_NAME, CLASSIC_BLOCKSCOUT),
			new NetworkInfo(XDAI_NETWORK_NAME,
							xDAI_SYMBOL,
							XDAI_RPC_URL,
							"https://blockscout.com/poa/dai/tx/",
							XDAI_ID,
							false,
							"https://dai.poa.network",
							"https://blockscout.com/poa/dai/", XDAI_TICKER_NAME, XDAI_BLOCKSCOUT),
      new NetworkInfo(POA_NETWORK_NAME, POA_SYMBOL,
                    POA_RPC_URL,
                    "https://poaexplorer.com/txid/search/", POA_ID, false, ETHEREUM_TICKER_NAME, POA_BLOCKSCOUT),
			new NetworkInfo(ARTIS_SIGMA1_NETWORK, ARTIS_SIGMA1_SYMBOL, ARTIS_SIGMA1_RPC_URL,
							"https://explorer.sigma1.artis.network/tx/", ARTIS_SIGMA1_ID, false,
							ARTIS_SIGMA1_RPC_URL,
							"https://explorer.sigma1.artis.network/", ARTIS_SIGMA_TICKER, ""),
			new NetworkInfo(KOVAN_NETWORK_NAME, ETH_SYMBOL, KOVAN_RPC_URL,
                    "https://kovan.etherscan.io/tx/", KOVAN_ID, false,
							"https://kovan.infura.io/v3/da3717f25f824cc1baa32d812386d93f",
							"https://api-kovan.etherscan.io/", ETHEREUM_TICKER_NAME, KOVAN_BLOCKSCOUT),
			new NetworkInfo(ROPSTEN_NETWORK_NAME, ETH_SYMBOL,
							ROPSTEN_RPC_URL,
                    "https://ropsten.etherscan.io/tx/",ROPSTEN_ID, false,
							"https://ropsten.infura.io/v3/da3717f25f824cc1baa32d812386d93f",
					"https://api-ropsten.etherscan.io/", ETHEREUM_TICKER_NAME, ROPSTEN_BLOCKSCOUT),
            new NetworkInfo(SOKOL_NETWORK_NAME, POA_SYMBOL,
                    SOKOL_RPC_URL,
                    "https://sokol-explorer.poa.network/account/",SOKOL_ID, false, ETHEREUM_TICKER_NAME, SOKOL_BLOCKSCOUT),
			new NetworkInfo(RINKEBY_NETWORK_NAME, ETH_SYMBOL, RINKEBY_RPC_URL,
							"https://rinkeby.etherscan.io/tx/",RINKEBY_ID, false,
							"https://rinkeby.infura.io/v3/da3717f25f824cc1baa32d812386d93f",
              "https://api-rinkeby.etherscan.io/", ETHEREUM_TICKER_NAME, RINKEBY_BLOCKSCOUT),
			new NetworkInfo(GOERLI_NETWORK_NAME, GOERLI_SYMBOL, GOERLI_RPC_URL,
							"https://goerli.etherscan.io/tx/",GOERLI_ID, false,
							GOERLI_RPC_URL,
							"https://api-goerli.etherscan.io/", ETHEREUM_TICKER_NAME, GOERLI_BLOCKSCOUT),
			new NetworkInfo(ARTIS_TAU1_NETWORK, ARTIS_TAU1_SYMBOL, ARTIS_TAU1_RPC_URL,
							"https://explorer.tau1.artis.network/tx/", ARTIS_TAU1_ID, false,
							ARTIS_TAU1_RPC_URL,
							"https://explorer.tau1.artis.network/", ARTIS_SIGMA_TICKER, ""),
	};

	private final PreferenceRepositoryType preferences;
    private final TickerService tickerService;
    private NetworkInfo defaultNetwork;
    private final Set<OnNetworkChangeListener> onNetworkChangedListeners = new HashSet<>();

    public EthereumNetworkRepository(PreferenceRepositoryType preferenceRepository, TickerService tickerService) {
		this.preferences = preferenceRepository;
		this.tickerService = tickerService;
		defaultNetwork = getByName(preferences.getDefaultNetwork());
		if (defaultNetwork == null) {
			defaultNetwork = NETWORKS[0];
		}

		networkMap = new ConcurrentHashMap<>();
		for (NetworkInfo network : NETWORKS)
		{
			networkMap.put(network.chainId, network);
		}
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
		List<Integer> networkIds;
		String filterList = preferences.getNetworkFilterList();
		if (filterList.length() == 0)
		{
			networkIds = new ArrayList<>();
			//populate
			for (NetworkInfo network : NETWORKS)
			{
				if (hasRealValue(network.chainId))
				{
					networkIds.add(network.chainId);
				}
			}
		}
		else
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
    public Single<Ticker> getTicker(int chainId) {
    	NetworkInfo network = networkMap.get(chainId);
    	switch (network.tickerId)
		{
			case ARTIS_SIGMA_TICKER:
				return tickerService.convertPair("EUR", "USD")
						.map(this::getSigmaTicker);
			default:
				return Single.fromObservable(tickerService
						.fetchTickerPrice(network.tickerId));
		}
    }

	private Ticker getSigmaTicker(double rate)
	{
		Ticker artisTicker = new Ticker();
		artisTicker.percentChange24h = "0.00";
		double conversion = (1.0/13.7603)*rate; //13.7603 ATS = 1 EUR
		artisTicker.price_usd = String.valueOf(conversion);
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
}
