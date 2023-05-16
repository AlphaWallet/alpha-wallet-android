package com.alphawallet.app.repository;

import com.alphawallet.app.entity.CurrencyItem;

import java.util.Set;

public interface PreferenceRepositoryType
{
    String getCurrentWalletAddress();

    void setCurrentWalletAddress(String address);

    long getActiveBrowserNetwork();

    void setActiveBrowserNetwork(long networkId);

    String getNetworkFilterList();

    void setNetworkFilterList(String filters);

    String getCustomRPCNetworks();

    void setCustomRPCNetworks(String networks);

    String getDefaultLocale();

    boolean isFindWalletAddressDialogShown();

    void setFindWalletAddressDialogShown(boolean isShown);

    String getDefaultCurrency();

    void setDefaultCurrency(CurrencyItem currency);

    String getDefaultCurrencySymbol();

    String getUserPreferenceLocale();

    void setUserPreferenceLocale(String locale);

    boolean getFullScreenState();

    void setFullScreenState(boolean state);

    boolean getUse1559Transactions();

    void setUse1559Transactions(boolean toggleState);

    boolean isTestnetEnabled();

    void setTestnetEnabled(boolean enabled);

    String getPriceAlerts();

    void setPriceAlerts(String json);

    void setHasSetNetworkFilters();

    boolean hasSetNetworkFilters();

    void blankHasSetNetworkFilters();

    void commit();

    void incrementLaunchCount();

    int getLaunchCount();

    void resetLaunchCount();

    void setRateAppShown();

    boolean getRateAppShown();

    void setShowZeroBalanceTokens(boolean shouldShow);

    boolean shouldShowZeroBalanceTokens();

    int getUpdateWarningCount();

    void setUpdateWarningCount(int count);

    long getInstallTime();

    void setInstallTime(long time);

    String getUniqueId();

    void setUniqueId(String uuid);

    boolean isMarshMallowWarningShown();

    void setMarshMallowWarning(boolean shown);

    void storeLastFragmentPage(int ordinal);

    int getLastFragmentPage();

    int getLastVersionCode(int currentCode);

    void setLastVersionCode(int code);

    int getTheme();

    void setTheme(int state);

    boolean isNewWallet(String address);

    void setNewWallet(String address, boolean isNewWallet);
    void setWatchOnly(boolean watchOnly);

    boolean isWatchOnly();

    Set<String> getSelectedSwapProviders();

    void setSelectedSwapProviders(Set<String> swapProviders);

    boolean isAnalyticsEnabled();

    void setAnalyticsEnabled(boolean isEnabled);

    boolean isCrashReportingEnabled();

    void setCrashReportingEnabled(boolean isEnabled);

    long getLoginTime(String address);

    void logIn(String address);

    void setFirebaseMessagingToken(String token);

    String getFirebaseMessagingToken();

    boolean isTransactionNotificationsEnabled(String address);

    void setTransactionNotificationEnabled(String address, boolean isEnabled);

    boolean isPostNotificationsPermissionRequested(String address);

    void setPostNotificationsPermissionRequested(String address, boolean hasRequested);
}
