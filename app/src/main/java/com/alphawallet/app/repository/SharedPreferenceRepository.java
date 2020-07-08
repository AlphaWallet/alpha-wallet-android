package com.alphawallet.app.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.CurrencyItem;

import java.util.Locale;

public class SharedPreferenceRepository implements PreferenceRepositoryType {
    private static final String CURRENT_ACCOUNT_ADDRESS_KEY = "current_account_address";
    private static final String DEFAULT_NETWORK_NAME_KEY = "default_network_name";
    private static final String NETWORK_FILTER_KEY = "network_filters";
    private static final String GAS_PRICE_KEY = "gas_price";
    private static final String GAS_LIMIT_KEY = "gas_limit";
    private static final String GAS_LIMIT_FOR_TOKENS_KEY = "gas_limit_for_tokens";
    private static final String NOTIFICATIONS_KEY = "notifications";
    private static final String DEFAULT_SET_KEY = "default_net_set";
    private static final String LOCALE_KEY = "locale";
    private static final String BACKUP_WALLET_SHOWN = "backup_wallet_shown";
    private static final String FIND_WALLET_ADDRESS_SHOWN = "find_wallet_address_shown";
    private static final String CURRENCY_CODE_KEY = "currency_locale";
    private static final String CURRENCY_SYMBOL_KEY = "currency_symbol";
    private static final String IS_SPEED_UP_TIP_SHOWN = "speedup_tip";

    private final SharedPreferences pref;

    public SharedPreferenceRepository(Context context) {
        pref = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public String getCurrentWalletAddress() {
        return pref.getString(CURRENT_ACCOUNT_ADDRESS_KEY, null);
    }

    @Override
    public void setCurrentWalletAddress(String address) {
        pref.edit().putString(CURRENT_ACCOUNT_ADDRESS_KEY, address).apply();
    }

    @Override
    public String getDefaultNetwork() {
        return pref.getString(DEFAULT_NETWORK_NAME_KEY, null);
    }

    @Override
    public void setDefaultNetwork(String netName) {
        pref.edit().putString(DEFAULT_NETWORK_NAME_KEY, netName).apply();
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
    public boolean getDefaultNetworkSet() {
        return pref.getBoolean(DEFAULT_SET_KEY, false);
    }

    @Override
    public void setDefaultNetworkSet() {
        pref.edit().putBoolean(DEFAULT_SET_KEY, true).apply();
    }

    @Override
    public String getDefaultLocale() {
        return pref.getString(LOCALE_KEY, Locale.getDefault().getLanguage());
    }

    @Override
    public void setDefaultLocale(String locale) {
        pref.edit().putString(LOCALE_KEY, locale).apply();
    }

    @Override
    public boolean isBackupWalletDialogShown() {
        return pref.getBoolean(BACKUP_WALLET_SHOWN, false);
    }

    @Override
    public void setBackupWalletDialogShown(boolean isShown) {
        pref.edit().putBoolean(BACKUP_WALLET_SHOWN, isShown).apply();
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
        pref.edit().putString(CURRENCY_CODE_KEY, currencyItem.getCode()).apply();
        pref.edit().putString(CURRENCY_SYMBOL_KEY, currencyItem.getSymbol()).apply();
    }

    @Override
    public String getDefaultCurrency() {
        return pref.getString(CURRENCY_CODE_KEY, C.DEFAULT_CURRENCY_CODE);
    }

    @Override
    public boolean isSpeedUpTipShown() {
        return pref.getBoolean(IS_SPEED_UP_TIP_SHOWN, false);
    }

    @Override
    public void setSpeedUpTipShown() {
        pref.edit().putBoolean(IS_SPEED_UP_TIP_SHOWN, true).apply();
    }
}
