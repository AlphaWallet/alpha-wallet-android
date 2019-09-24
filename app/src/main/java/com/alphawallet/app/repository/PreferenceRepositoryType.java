package com.alphawallet.app.repository;

public interface PreferenceRepositoryType {
    String getCurrentWalletAddress();

    void setCurrentWalletAddress(String address);

    String getDefaultNetwork();

    void setDefaultNetwork(String netName);

    void setNetworkFilterList(String filters);
    String getNetworkFilterList();

    boolean getNotificationsState();

    void setNotificationState(boolean state);

    boolean getDefaultNetworkSet();

    void setDefaultNetworkSet();

    String getDefaultLocale();

    void setDefaultLocale(String locale);

    boolean isBackupWalletDialogShown();

    void setBackupWalletDialogShown(boolean isShown);

    boolean isFindWalletAddressDialogShown();

    void setFindWalletAddressDialogShown(boolean isShown);
}
