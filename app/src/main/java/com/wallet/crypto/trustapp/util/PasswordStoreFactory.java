package com.wallet.crypto.trustapp.util;

import android.app.Activity;
import android.content.Context;
import android.os.Build;

import com.wallet.crypto.trustapp.controller.ServiceErrorException;
import com.wallet.crypto.trustapp.views.TransactionListActivity;
import com.wallet.pwd.trustapp.PasswordManager;

public class PasswordStoreFactory {
	public static void put(Context context, String address, String password) throws ServiceErrorException {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			KS.put(context, password, address);
		} else {
			try {
				PasswordManager.setPassword(address, password, context);
			} catch (Exception e) {
				throw new ServiceErrorException(ServiceErrorException.KEY_STORE_ERROR);
			}
		}
	}

	public static byte[] get(Context context, String address) throws ServiceErrorException {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			return KS.get(context, address);
		} else {
			try {
				return PasswordManager.getPassword(address, context).getBytes();
			} catch (Exception e) {
				throw new ServiceErrorException(ServiceErrorException.KEY_STORE_ERROR);
			}
		}
	}

	public static void showAuthenticationScreen(Context context, int unlockScreenRequest) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			KS.showAuthenticationScreen(context, unlockScreenRequest);
		}
	}
}
