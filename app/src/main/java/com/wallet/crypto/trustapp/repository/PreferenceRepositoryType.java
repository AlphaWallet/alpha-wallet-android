package com.wallet.crypto.trustapp.repository;

import com.wallet.crypto.trustapp.entity.GasSettings;

import io.reactivex.Single;

public interface PreferenceRepositoryType {
	String getCurrentWalletAddress();
	void setCurrentWalletAddress(String address);

	String getDefaultNetwork();
	void setDefaultNetwork(String netName);

	GasSettings getGasSettings();
	void setGasSettings(GasSettings gasPrice);

}
