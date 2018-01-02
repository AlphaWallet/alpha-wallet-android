package com.wallet.crypto.trustapp.repository;

import com.wallet.crypto.trustapp.entity.NetworkInfo;
import com.wallet.crypto.trustapp.entity.Ticker;

import io.reactivex.Single;

public interface EthereumNetworkRepositoryType {

	NetworkInfo getDefaultNetwork();

	void setDefaultNetworkInfo(NetworkInfo networkInfo);

	NetworkInfo[] getAvailableNetworkList();

	void addOnChangeDefaultNetwork(OnNetworkChangeListener onNetworkChanged);

	Single<Ticker> getTicker();
}
