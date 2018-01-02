package com.wallet.crypto.trustapp.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.wallet.crypto.trustapp.entity.ServiceErrorException;
import com.wallet.pwd.trustapp.PasswordManager;

import java.util.Map;

public class PMMigrateHelper {
	public static void migrate(Context context) throws ServiceErrorException {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			return;
		}
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		Map<String, ?> passwords = pref.getAll();
		for (String key : passwords.keySet()) {
			if (key.contains("-pwd")) {
				String address = key.replace("-pwd", "");
				try {
					KS.put(context, address.toLowerCase(), PasswordManager.getPassword(address, context));
				} catch (ServiceErrorException ex) {
					throw ex;
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}

	}
}
