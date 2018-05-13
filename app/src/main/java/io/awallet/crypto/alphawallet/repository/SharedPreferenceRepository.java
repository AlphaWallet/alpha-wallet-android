package io.awallet.crypto.alphawallet.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SharedPreferenceRepository implements PreferenceRepositoryType {

	private static final String CURRENT_ACCOUNT_ADDRESS_KEY = "current_account_address";
	private static final String DEFAULT_NETWORK_NAME_KEY = "default_network_name";
	private static final String GAS_PRICE_KEY  ="gas_price";
    private static final String GAS_LIMIT_KEY  ="gas_limit";
	private static final String GAS_LIMIT_FOR_TOKENS_KEY = "gas_limit_for_tokens";
	private static final String NOTIFICATIONS_KEY = "notifications";
	private static final String DEFAULT_SET_KEY = "default_net_set";
	private static final String LOCALE_KEY = "locale";
	private static final String LOCALE_CODE_KEY = "locale_code";

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
	public boolean getNotificationsState()
	{
		return pref.getBoolean(NOTIFICATIONS_KEY, true);
	}

	@Override
	public void setNotificationState(boolean state)
	{
		pref.edit().putBoolean(NOTIFICATIONS_KEY, state).apply();
	}

	@Override
	public boolean getDefaultNetworkSet()
	{
		return pref.getBoolean(DEFAULT_SET_KEY, false);
	}
	@Override
	public void setDefaultNetworkSet()
	{
		pref.edit().putBoolean(DEFAULT_SET_KEY, true).apply();
	}

	@Override
	public String getDefaultLocale() {
		return pref.getString(LOCALE_KEY, "English");
	}

	@Override
	public void setDefaultLocale(String locale) {
		pref.edit().putString(LOCALE_KEY, locale).apply();
	}

	@Override
	public String getDefaultLocaleCode() {
		return pref.getString(LOCALE_CODE_KEY, "en");
	}

	@Override
	public void setDefaultLocaleCode(String localeCode) {
		pref.edit().putString(LOCALE_CODE_KEY, localeCode).apply();
	}
}
