package com.wallet.crypto.trustapp.repository;

import com.wallet.crypto.trustapp.entity.NetworkInfo;

public interface EtheriumNetworkRepositoryType {
	NetworkInfo getDefaultNetwork();

	void addOnChangeDefaultNetwork(OnNetworkChangeListener onNetworkChanged);
}
