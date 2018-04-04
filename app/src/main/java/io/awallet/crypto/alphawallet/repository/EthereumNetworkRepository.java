package io.awallet.crypto.alphawallet.repository;

import android.text.TextUtils;

import io.awallet.crypto.alphawallet.entity.NetworkInfo;
import io.awallet.crypto.alphawallet.entity.Ticker;
import io.awallet.crypto.alphawallet.service.TickerService;

import java.util.HashSet;
import java.util.Set;

import io.reactivex.Single;

import static io.awallet.crypto.alphawallet.C.CLASSIC_NETWORK_NAME;
import static io.awallet.crypto.alphawallet.C.ETC_SYMBOL;
import static io.awallet.crypto.alphawallet.C.ETHEREUM_NETWORK_NAME;
import static io.awallet.crypto.alphawallet.C.ETH_SYMBOL;
import static io.awallet.crypto.alphawallet.C.KOVAN_NETWORK_NAME;
import static io.awallet.crypto.alphawallet.C.POA_NETWORK_NAME;
import static io.awallet.crypto.alphawallet.C.POA_SYMBOL;
import static io.awallet.crypto.alphawallet.C.ROPSTEN_NETWORK_NAME;
import static io.awallet.crypto.alphawallet.C.SOKOL_NETWORK_NAME;

public class EthereumNetworkRepository implements EthereumNetworkRepositoryType {


	private final NetworkInfo[] NETWORKS = new NetworkInfo[] {
			new NetworkInfo(ETHEREUM_NETWORK_NAME, ETH_SYMBOL,
                    "https://mainnet.infura.io/llyrtzQ3YhkdESt2Fzrk",
                    "https://api.trustwalletapp.com/",
                    "https://etherscan.io/tx/",1, true,
							"http://stormbird.duckdns.org:8540/",
							"https://api.etherscan.io/"),
            new NetworkInfo(CLASSIC_NETWORK_NAME, ETC_SYMBOL,
                    "https://mewapi.epool.io/",
                    "https://classic.trustwalletapp.com",
                    "https://gastracker.io/tx/",61, true),
            new NetworkInfo(POA_NETWORK_NAME, POA_SYMBOL,
                    "https://core.poa.network/",
                    "https://poa.trustwalletapp.com",
                    "https://poaexplorer.com/txid/search/", 99, false),
			new NetworkInfo(KOVAN_NETWORK_NAME, ETH_SYMBOL,
                    "https://kovan.infura.io/llyrtzQ3YhkdESt2Fzrk",
                    "https://kovan.trustwalletapp.com/",
                    "https://kovan.etherscan.io/tx/", 42, false,
							null,
							"https://api-kovan.etherscan.io/"),
			new NetworkInfo(ROPSTEN_NETWORK_NAME, ETH_SYMBOL,
                    "https://ropsten.infura.io/LY55hqqffzZcQ0b513JJ",
					//"http://stormbird.duckdns.org:8545/",
                    "https://ropsten.trustwalletapp.com/",
                    "https://ropsten.etherscan.io/tx/",3, false,
					"http://stormbird.duckdns.org:8545/",
					"https://api-ropsten.etherscan.io/"),
            new NetworkInfo(SOKOL_NETWORK_NAME, POA_SYMBOL,
                    "https://sokol.poa.network",
                    "https://trust-sokol.herokuapp.com/",
                    "https://sokol-explorer.poa.network/account/",77, false),
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
    public Single<Ticker> getTicker() {
        return Single.fromObservable(tickerService
                .fetchTickerPrice(getByName(ETHEREUM_NETWORK_NAME).name));
    }
}
