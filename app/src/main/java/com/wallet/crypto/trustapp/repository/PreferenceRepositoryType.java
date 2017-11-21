package com.wallet.crypto.trustapp.repository;

public interface PreferenceRepositoryType {
	String getCurrentAccountAddress();
	void setCurrentAccountAddress(String address);

	String getDefaultNetwork();
	void setDefaultNetwork(String netName);
}
