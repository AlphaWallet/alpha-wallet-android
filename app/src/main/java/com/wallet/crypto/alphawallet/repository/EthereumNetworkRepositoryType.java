package com.wallet.crypto.alphawallet.repository;

import com.wallet.crypto.alphawallet.entity.NetworkInfo;
import com.wallet.crypto.alphawallet.entity.Ticker;

import io.reactivex.Single;

public interface EthereumNetworkRepositoryType {

	NetworkInfo getDefaultNetwork();

	void setDefaultNetworkInfo(NetworkInfo networkInfo);

	NetworkInfo[] getAvailableNetworkList();

	void addOnChangeDefaultNetwork(OnNetworkChangeListener onNetworkChanged);

	Single<Ticker> getTicker();
}
