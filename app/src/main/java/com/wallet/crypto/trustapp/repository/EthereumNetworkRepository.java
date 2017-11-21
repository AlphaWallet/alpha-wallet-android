package com.wallet.crypto.trustapp.repository;

import android.text.TextUtils;

import com.wallet.crypto.trustapp.entity.NetworkInfo;

public class EthereumNetworkRepository implements EthereumNetworkRepositoryType {
	
	private final NetworkInfo[] NETWORKS = new NetworkInfo[] {
			new NetworkInfo("mainnet", "https://mainnet.infura.io/llyrtzQ3YhkdESt2Fzrk", "https://api.etherscan.io", "ZVU87DFQYV2TPJQKRJDITS42MW58GUEZ4V", 1),
			new NetworkInfo("kovan", "https://kovan.infura.io/llyrtzQ3YhkdESt2Fzrk", "https://kovan.etherscan.io", "ZVU87DFQYV2TPJQKRJDITS42MW58GUEZ4V", 42),
			new NetworkInfo("ropstein", "https://ropstein.infura.io/llyrtzQ3YhkdESt2Fzrk", "https://ropstein.etherscan.io", "ZVU87DFQYV2TPJQKRJDITS42MW58GUEZ4V", 3),
			new NetworkInfo("rinkeby", "https://rinkeby.infura.io/llyrtzQ3YhkdESt2Fzrk", "https://rinkeby.etherscan.io", "ZVU87DFQYV2TPJQKRJDITS42MW58GUEZ4V", 4)
	};

	private final PreferenceRepositoryType preferences;
	private NetworkInfo defaultNetwork;

	public EthereumNetworkRepository(PreferenceRepositoryType preferenceRepository) {
		this.preferences = preferenceRepository;
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
	}

	@Override
	public NetworkInfo[] getAvailableNetworkList() {
		return NETWORKS;
	}

	@Override
	public void addOnChangeDefaultNetwork(OnNetworkChangeListener onNetworkChanged) {

	}
}
