package com.wallet.crypto.trustapp.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.wallet.crypto.trustapp.controller.ServiceErrorException;
import com.wallet.pwd.trustapp.PasswordManager;

import java.util.Map;

public class PMMigrateHelper {
	public static void migrate(Context context) throws ServiceErrorException {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		Map<String, ?> passwords = pref.getAll();
		for (String key : passwords.keySet()) {
			if (key.contains("-pwd")) {
				String address = key.replace("-pwd", "");
				try {
					KS.put(context, address.toLowerCase(), PasswordManager.getPassword(address, context));
					pref.edit().remove(key).apply();
				} catch (ServiceErrorException ex) {
					throw ex;
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}

	}
}
