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

import static io.stormbird.wallet.C.CLASSIC_NETWORK_NAME;
import static io.stormbird.wallet.C.ETC_SYMBOL;
import static io.stormbird.wallet.C.ETHEREUM_NETWORK_NAME;
import static io.stormbird.wallet.C.ETH_SYMBOL;
import static io.stormbird.wallet.C.KOVAN_NETWORK_NAME;
import static io.stormbird.wallet.C.POA_NETWORK_NAME;
import static io.stormbird.wallet.C.POA_SYMBOL;
import static io.stormbird.wallet.C.RINKEBY_NETWORK_NAME;
import static io.stormbird.wallet.C.ROPSTEN_NETWORK_NAME;
import static io.stormbird.wallet.C.SOKOL_NETWORK_NAME;

public class EthereumNetworkRepository implements EthereumNetworkRepositoryType {

	private final NetworkInfo[] NETWORKS = new NetworkInfo[] {
			new NetworkInfo(ETHEREUM_NETWORK_NAME, ETH_SYMBOL,
                    "https://mainnet.infura.io/v3/da3717f25f824cc1baa32d812386d93f",
                    "https://etherscan.io/tx/",1, true,
							"https://mainnet.infura.io/v3/da3717f25f824cc1baa32d812386d93f",
							"https://api.etherscan.io/"),
           new NetworkInfo(CLASSIC_NETWORK_NAME, ETC_SYMBOL,
                    "https://mewapi.epool.io/",
                    "https://gastracker.io/tx/",61, true),
            new NetworkInfo(POA_NETWORK_NAME, POA_SYMBOL,
                    "https://core.poa.network/",
                    "https://poaexplorer.com/txid/search/", 99, false),
			new NetworkInfo(KOVAN_NETWORK_NAME, ETH_SYMBOL,
                    "https://kovan.infura.io/v3/da3717f25f824cc1baa32d812386d93f",
                    "https://kovan.etherscan.io/tx/", 42, false,
							"https://kovan.infura.io/v3/da3717f25f824cc1baa32d812386d93f",
							"https://api-kovan.etherscan.io/"),
			new NetworkInfo(ROPSTEN_NETWORK_NAME, ETH_SYMBOL,
							"https://ropsten.infura.io/v3/da3717f25f824cc1baa32d812386d93f",
                    "https://ropsten.etherscan.io/tx/",3, false,
							"https://ropsten.infura.io/v3/da3717f25f824cc1baa32d812386d93f",
					"https://api-ropsten.etherscan.io/"),
            new NetworkInfo(SOKOL_NETWORK_NAME, POA_SYMBOL,
                    "https://sokol.poa.network",
                    "https://sokol-explorer.poa.network/account/",77, false),
			new NetworkInfo(RINKEBY_NETWORK_NAME, ETH_SYMBOL,
							"https://rinkeby.infura.io/v3/da3717f25f824cc1baa32d812386d93f",
							"https://rinkeby.etherscan.io/tx/",4, false,
							"https://rinkeby.infura.io/v3/da3717f25f824cc1baa32d812386d93f",
							"https://api-rinkeby.etherscan.io/"),
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

	@Override
	public void setActiveRPC(String rpcURL)
	{
		currentActiveRPC = rpcURL;
	}

	@Override
	public String getActiveRPC()
	{
		if (currentActiveRPC != null)
		{
			return currentActiveRPC;
		}
		else
		{
			return defaultNetwork.rpcServerUrl;
		}
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
                .fetchTickerPrice(getByName(ETHEREUM_NETWORK_NAME).name));
    }
}
