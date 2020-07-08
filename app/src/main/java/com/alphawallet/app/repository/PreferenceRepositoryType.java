package com.alphawallet.app.repository;

import com.alphawallet.app.entity.CurrencyItem;

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

    void setDefaultCurrency(CurrencyItem currency);

    String getDefaultCurrency();

    boolean isSpeedUpTipShown();

    void setSpeedUpTipShown();
}
