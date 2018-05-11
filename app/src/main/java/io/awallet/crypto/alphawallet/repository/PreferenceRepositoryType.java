package io.awallet.crypto.alphawallet.repository;

public interface PreferenceRepositoryType {
	String getCurrentWalletAddress();
	void setCurrentWalletAddress(String address);

	String getDefaultNetwork();
	void setDefaultNetwork(String netName);

	boolean getNotificationsState();
	void setNotificationState(boolean state);

	boolean getDefaultNetworkSet();
	void setDefaultNetworkSet();
}
