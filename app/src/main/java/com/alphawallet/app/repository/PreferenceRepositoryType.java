package com.alphawallet.app.repository;

import com.alphawallet.app.entity.CurrencyItem;

public interface PreferenceRepositoryType {
    String getCurrentWalletAddress();

    void setCurrentWalletAddress(String address);

    String getActiveBrowserNetwork();

    void setActiveBrowserNetwork(String netName);

    String getNetworkFilterList();

    void setNetworkFilterList(String filters);

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

    void setPriceAlerts(String json);

    String getPriceAlerts();
}
