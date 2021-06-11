package com.alphawallet.app.repository;

import com.alphawallet.app.entity.CurrencyItem;

public interface PreferenceRepositoryType {
    String getCurrentWalletAddress();

    void setCurrentWalletAddress(String address);

    int getActiveBrowserNetwork();

    void setActiveBrowserNetwork(int networkId);

    String getNetworkFilterList();

    void setNetworkFilterList(String filters);

    boolean getNotificationsState();

    void setNotificationState(boolean state);

    String getDefaultLocale();

    boolean isFindWalletAddressDialogShown();

    void setFindWalletAddressDialogShown(boolean isShown);

    String getDefaultCurrency();

    void setDefaultCurrency(CurrencyItem currency);

    String getUserPreferenceLocale();

    void setUserPreferenceLocale(String locale);

    boolean getFullScreenState();

    void setFullScreenState(boolean state);

    boolean isActiveMainnet();

    void setActiveMainnet(boolean state);

    boolean hasShownTestNetWarning();

    void setShownTestNetWarning();

    void setHasSetNetworkFilters();
    boolean hasSetNetworkFilters();
    void blankHasSetNetworkFilters();

    void commit();
}
