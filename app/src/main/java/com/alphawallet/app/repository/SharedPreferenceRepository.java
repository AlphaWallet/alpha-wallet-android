package com.alphawallet.app.repository;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.CurrencyItem;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class SharedPreferenceRepository implements PreferenceRepositoryType {
    private static final String CURRENT_ACCOUNT_ADDRESS_KEY = "current_account_address";
    private static final String DEFAULT_NETWORK_NAME_KEY = "default_network_name";
    private static final String NETWORK_FILTER_KEY = "network_filters";
    private static final String CUSTOM_NETWORKS_KEY = "custom_networks";
    private static final String NOTIFICATIONS_KEY = "notifications";
    private static final String THEME_KEY = "theme";
    private static final String DEFAULT_SET_KEY = "default_net_set";
    private static final String LOCALE_KEY = "locale";
    private static final String BACKUP_WALLET_SHOWN = "backup_wallet_shown";
    private static final String FIND_WALLET_ADDRESS_SHOWN = "find_wallet_address_shown";
    public static final String CURRENCY_CODE_KEY = "currency_locale";
    public static final String CURRENCY_SYMBOL_KEY = "currency_symbol";
    public static final String USER_LOCALE_PREF = "user_locale_pref";
    public static final String HIDE_ZERO_BALANCE_TOKENS = "hide_zero_balance_tokens";
    public static final String FULL_SCREEN_STATE = "full_screen";
    public static final String EXPERIMENTAL_1559_TX = "ex_1559_tx";
    public static final String ACTIVE_MAINNET = "active_mainnet";
    public static final String SHOWN_WARNING = "shown_warning";
    public static final String PRICE_ALERTS = "price_alerts";
    private static final String SET_NETWORK_FILTERS = "set_filters";
    private static final String SHOULD_SHOW_ROOT_WARNING = "should_show_root_warning";
    private static final String UPDATE_WARNINGS = "update_warns";
    private static final String INSTALL_TIME = "install_time";
    public static final String DEVICE_LOCALE = "device_locale";
    public static final String DEVICE_COUNTRY = "device_country";
    public static final String MARSHMALLOW_SUPPORT_WARNING = "marshmallow_version_support_warning_shown";
    private static final String LAST_FRAGMENT_ID = "lastfrag_id";
    private static final String LAST_VERSION_CODE = "last_version_code";
    private static final String SELECTED_SWAP_PROVIDERS_KEY = "selected_exchanges";

    private static final String RATE_APP_SHOWN = "rate_us_shown";
    private static final String LAUNCH_COUNT = "launch_count";
    private static final String NEW_WALLET = "new_wallet_";

    private final SharedPreferences pref;

    public SharedPreferenceRepository(Context context) {
        pref = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public String getCurrentWalletAddress() {
        return pref.getString(CURRENT_ACCOUNT_ADDRESS_KEY, null);
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void setCurrentWalletAddress(String address) {
        pref.edit().putString(CURRENT_ACCOUNT_ADDRESS_KEY, address).commit(); //use commit as the value may be used immediately
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public long getActiveBrowserNetwork() {
        long selectedNetwork;
        try
        {
            selectedNetwork = pref.getLong(DEFAULT_NETWORK_NAME_KEY, 0);
        }
        catch (ClassCastException e) //previously we used Integer or String
        {
            try
            {
                selectedNetwork = pref.getInt(DEFAULT_NETWORK_NAME_KEY, 0);
            }
            catch (ClassCastException string)
            {
                selectedNetwork = EthereumNetworkRepository.getNetworkIdFromName(pref.getString(DEFAULT_NETWORK_NAME_KEY, ""));
            }
            pref.edit().putLong(DEFAULT_NETWORK_NAME_KEY, selectedNetwork).commit(); //commit as we need to update this immediately
        }

        return selectedNetwork;
    }

    @Override
    public void setActiveBrowserNetwork(long networkId) {
        pref.edit().putLong(DEFAULT_NETWORK_NAME_KEY, networkId).apply();
    }

    @Override
    public String getCustomRPCNetworks() {
        return pref.getString(CUSTOM_NETWORKS_KEY, "");
    }

    @Override
    public void setCustomRPCNetworks(String networks) {
        pref.edit().putString(CUSTOM_NETWORKS_KEY, networks).apply();
    }

    @Override
    public void setNetworkFilterList(String filters)
    {
        pref.edit().putString(NETWORK_FILTER_KEY, filters).apply();
    }

    @Override
    public String getNetworkFilterList()
    {
        return pref.getString(NETWORK_FILTER_KEY, "");
    }

    @Override
    public boolean getNotificationsState() {
        return pref.getBoolean(NOTIFICATIONS_KEY, true);
    }

    @Override
    public void setNotificationState(boolean state) {
        pref.edit().putBoolean(NOTIFICATIONS_KEY, state).apply();
    }

    @Override
    public String getDefaultLocale() {
        return pref.getString(LOCALE_KEY, Locale.getDefault().getLanguage());
    }

    @Override
    public boolean isFindWalletAddressDialogShown() {
        return pref.getBoolean(FIND_WALLET_ADDRESS_SHOWN, false);
    }

    @Override
    public void setFindWalletAddressDialogShown(boolean isShown) {
        pref.edit().putBoolean(FIND_WALLET_ADDRESS_SHOWN, isShown).apply();
    }

    @Override
    public void setDefaultCurrency(CurrencyItem currencyItem) {
        if (currencyItem == null) return;
        pref.edit().putString(CURRENCY_CODE_KEY, currencyItem.getCode()).apply();
        pref.edit().putString(CURRENCY_SYMBOL_KEY, currencyItem.getSymbol()).apply();
    }

    @Override
    public String getDefaultCurrency() {
        return pref.getString(CURRENCY_CODE_KEY, C.DEFAULT_CURRENCY_CODE);
    }

    @Override
    public String getDefaultCurrencySymbol() {
        return pref.getString(CURRENCY_SYMBOL_KEY, C.DEFAULT_CURRENCY_CODE);
    }

    @Override
    public String getUserPreferenceLocale()
    {
        return pref.getString(USER_LOCALE_PREF, "");
    }

    @Override
    public void setUserPreferenceLocale(String locale)
    {
        pref.edit().putString(USER_LOCALE_PREF, locale).apply();
    }

    @Override
    public void setFullScreenState(boolean state)
    {
        pref.edit().putBoolean(FULL_SCREEN_STATE, state).apply();
    }


    @Override
    public boolean getFullScreenState()
    {
        return pref.getBoolean(FULL_SCREEN_STATE, false);
    }

    @Override
    public void setUse1559Transactions(boolean state)
    {
        pref.edit().putBoolean(EXPERIMENTAL_1559_TX, state).apply();
    }

    @Override
    public boolean getUse1559Transactions()
    {
        return pref.getBoolean(EXPERIMENTAL_1559_TX, false);
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void setActiveMainnet(boolean state) {
        pref.edit().putBoolean(ACTIVE_MAINNET, state).commit(); //use commit
    }

    @Override
    public boolean isActiveMainnet() {
        return pref.getBoolean(ACTIVE_MAINNET, true);
    }

    @Override
    public boolean hasShownTestNetWarning() {
        return pref.getBoolean(SHOWN_WARNING, false);
    }

    @Override
    public void setShownTestNetWarning() {
        pref.edit().putBoolean(SHOWN_WARNING, true).apply();
    }

    @Override
    public void setPriceAlerts(String json)
    {
        pref.edit().putString(PRICE_ALERTS, json).apply();
    }

    @Override
    public String getPriceAlerts()
    {
        return pref.getString(PRICE_ALERTS, "");
    }

    public void setHasSetNetworkFilters()
    {
        pref.edit().putBoolean(SET_NETWORK_FILTERS, true).apply();
    }

    @Override
    public boolean hasSetNetworkFilters()
    {
        return pref.getBoolean(SET_NETWORK_FILTERS, false);
    }

    @Override
    public void blankHasSetNetworkFilters()
    {
        pref.edit().putBoolean(SET_NETWORK_FILTERS, false).apply();
    }

    //Ensure settings are committed
    @SuppressLint("ApplySharedPref")
    @Override
    public void commit()
    {
        pref.edit().commit();
    }

    @Override
    public void incrementLaunchCount() {
        int prevLaunchCount = getLaunchCount();
        pref.edit().putInt(LAUNCH_COUNT, prevLaunchCount + 1).apply();
    }

    @Override
    public void resetLaunchCount() {
        pref.edit().putInt(LAUNCH_COUNT, 0).apply();
    }

    @Override
    public int getLaunchCount() {
        return pref.getInt(LAUNCH_COUNT, 0);
    }

    @Override
    public void setRateAppShown() {
        pref.edit().putBoolean(RATE_APP_SHOWN, true).apply();
    }

    @Override
    public boolean getRateAppShown() {
        return pref.getBoolean(RATE_APP_SHOWN, false);
    }

    @Override
    public boolean shouldShowZeroBalanceTokens() {
        return pref.getBoolean(HIDE_ZERO_BALANCE_TOKENS, false);
    }

    @Override
    public void setShowZeroBalanceTokens(boolean shouldShow) {
        pref.edit().putBoolean(HIDE_ZERO_BALANCE_TOKENS, shouldShow).apply();
    }

    @Override
    public int getUpdateWarningCount() {
        return pref.getInt(UPDATE_WARNINGS, 0);
    }

    @Override
    public void setUpdateWarningCount(int count) {
        pref.edit().putInt(UPDATE_WARNINGS, count).apply();
    }

    @Override
    public void setInstallTime(long time) {
        pref.edit().putLong(INSTALL_TIME, time).apply();
    }

    @Override
    public long getInstallTime() {
        return pref.getLong(INSTALL_TIME, 0);
    }

    @Override
    public String getUniqueId() {
        return pref.getString(C.PREF_UNIQUE_ID, "");
    }

    @Override
    public void setUniqueId(String uuid) {
        pref.edit().putString(C.PREF_UNIQUE_ID, uuid).apply();
    }

    @Override
    public boolean isMarshMallowWarningShown() {
        return pref.getBoolean(MARSHMALLOW_SUPPORT_WARNING, false);
    }

    @Override
    public void setMarshMallowWarning(boolean shown) {
        pref.edit().putBoolean(MARSHMALLOW_SUPPORT_WARNING, true).apply();
    }

    @Override
    public void storeLastFragmentPage(int ordinal)
    {
        pref.edit().putInt(LAST_FRAGMENT_ID, ordinal).apply();
    }

    @Override
    public int getLastFragmentPage()
    {
        return pref.getInt(LAST_FRAGMENT_ID, -1);
    }

    @Override
    public int getLastVersionCode(int currentCode) {
        int versionCode = pref.getInt(LAST_VERSION_CODE, 0);
        if (versionCode == 0)
        {
            setLastVersionCode(currentCode);
            versionCode = Integer.MAX_VALUE;
        }
        return versionCode; // First time users won't see the 'what's new' since the app is new, only start to see these on first update
    }

    @Override
    public void setLastVersionCode(int code) {
        pref.edit().putInt(LAST_VERSION_CODE, code).apply();
    }

    @Override
    public int getTheme()
    {
        return pref.getInt(THEME_KEY, C.THEME_AUTO);
    }

    @Override
    public void setTheme(int state)
    {
        pref.edit().putInt(THEME_KEY, state).apply();
    }

    @Override
    public boolean isNewWallet(String address)
    {
        return pref.getBoolean(keyOf(address), false);
    }

    @Override
    public void setNewWallet(String address, boolean isNewWallet)
    {
        pref.edit().putBoolean(keyOf(address), isNewWallet).apply();
    }

    @Override
    public Set<String> getSelectedSwapProviders()
    {
        return pref.getStringSet(SELECTED_SWAP_PROVIDERS_KEY, new HashSet<>());
    }

    @Override
    public void setSelectedSwapProviders(Set<String> swapProviders)
    {
        pref.edit().putStringSet(SELECTED_SWAP_PROVIDERS_KEY, swapProviders).apply();
    }

    @NonNull
    private String keyOf(String address)
    {
        return NEW_WALLET + address.toLowerCase(Locale.ENGLISH);
    }
}
