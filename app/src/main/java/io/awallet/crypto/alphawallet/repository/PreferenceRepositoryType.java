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

	String getDefaultLanguage();
	void setDefaultLanguage(String language);

	String getDefaultLanguageCode();
	void setDefaultLanguageCode(String languageCode);
}
