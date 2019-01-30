package io.stormbird.wallet.repository;

import android.text.TextUtils;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;

import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Ticker;
import io.stormbird.wallet.service.TickerService;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

import io.reactivex.Single;

import static io.stormbird.wallet.C.*;

public class EthereumNetworkRepository implements EthereumNetworkRepositoryType {

	public static final String MAINNET_RPC_URL = "https://mainnet.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
	public static final String CLASSIC_RPC_URL = "https://web3.gastracker.io";
	public static final String XDAI_RPC_URL = "https://dai.poa.network";
	public static final String POA_RPC_URL = "https://core.poa.network/";
	public static final String ROPSTEN_RPC_URL = "https://ropsten.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
	public static final String RINKEBY_RPC_URL = "https://rinkeby.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
	public static final String KOVAN_RPC_URL = "https://kovan.infura.io/v3/da3717f25f824cc1baa32d812386d93f";
	public static final String SOKOL_RPC_URL = "https://sokol.poa.network";

	private final NetworkInfo[] NETWORKS = new NetworkInfo[] {
			new NetworkInfo(ETHEREUM_NETWORK_NAME, ETH_SYMBOL,
                    MAINNET_RPC_URL,
                    "https://etherscan.io/tx/",1, true,
							"https://mainnet.infura.io/v3/da3717f25f824cc1baa32d812386d93f",
							"https://api.etherscan.io/",
							ETHEREUM_TICKER_NAME),
      new NetworkInfo(CLASSIC_NETWORK_NAME, ETC_SYMBOL,
                    CLASSIC_RPC_URL,
                    "https://gastracker.io/tx/",61, true, CLASSIC_TICKER_NAME),
			new NetworkInfo(XDAI_NETWORK_NAME,
							xDAI_SYMBOL,
							XDAI_RPC_URL,
							"https://blockscout.com/poa/dai/api",
							100,
							false,
							"https://dai.poa.network",
							"https://blockscout.com/poa/dai/tx", XDAI_TICKER_NAME),
      new NetworkInfo(POA_NETWORK_NAME, POA_SYMBOL,
                    POA_RPC_URL,
                    "https://poaexplorer.com/txid/search/", 99, false, ETHEREUM_TICKER_NAME),
			new NetworkInfo(KOVAN_NETWORK_NAME, ETH_SYMBOL, KOVAN_RPC_URL,
                    "https://kovan.etherscan.io/tx/", 42, false,
							"https://kovan.infura.io/v3/da3717f25f824cc1baa32d812386d93f",
							"https://api-kovan.etherscan.io/", ETHEREUM_TICKER_NAME),
			new NetworkInfo(ROPSTEN_NETWORK_NAME, ETH_SYMBOL,
							ROPSTEN_RPC_URL,
                    "https://ropsten.etherscan.io/tx/",3, false,
							"https://ropsten.infura.io/v3/da3717f25f824cc1baa32d812386d93f",
					"https://api-ropsten.etherscan.io/", ETHEREUM_TICKER_NAME),
            new NetworkInfo(SOKOL_NETWORK_NAME, POA_SYMBOL,
                    SOKOL_RPC_URL,
                    "https://sokol-explorer.poa.network/account/",77, false, ETHEREUM_TICKER_NAME),
			new NetworkInfo(RINKEBY_NETWORK_NAME, ETH_SYMBOL, RINKEBY_RPC_URL,
							"https://rinkeby.etherscan.io/tx/",4, false,
							"https://rinkeby.infura.io/v3/da3717f25f824cc1baa32d812386d93f",
              "https://api-rinkeby.etherscan.io/", ETHEREUM_TICKER_NAME),
	};

	private final PreferenceRepositoryType preferences;
    private final TickerService tickerService;
    private NetworkInfo defaultNetwork;
    private String currentActiveRPC;
    private final Set<OnNetworkChangeListener> onNetworkChangedListeners = new HashSet<>();

    public EthereumNetworkRepository(PreferenceRepositoryType preferenceRepository, TickerService tickerService) {
		this.preferences = preferenceRepository;
		this.tickerService = tickerService;
		defaultNetwork = getByName(preferences.getDefaultNetwork());
		if (defaultNetwork == null) {
			defaultNetwork = NETWORKS[0];
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
	public NetworkInfo getDefaultNetwork() {
		return defaultNetwork;
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
    public Single<Ticker> getTicker() {
        return Single.fromObservable(tickerService
                .fetchTickerPrice(defaultNetwork.tickerId));
    }
}
