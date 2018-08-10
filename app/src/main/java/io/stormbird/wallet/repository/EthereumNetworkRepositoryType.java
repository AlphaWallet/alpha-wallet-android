package io.stormbird.wallet.repository;

import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Ticker;

import io.reactivex.Single;

public interface EthereumNetworkRepositoryType {

	NetworkInfo getDefaultNetwork();

	String getActiveRPC();

	void setActiveRPC(String rpcURL);

	void setDefaultNetworkInfo(NetworkInfo networkInfo);

	NetworkInfo[] getAvailableNetworkList();

	void addOnChangeDefaultNetwork(OnNetworkChangeListener onNetworkChanged);

	Single<Ticker> getTicker();
}
