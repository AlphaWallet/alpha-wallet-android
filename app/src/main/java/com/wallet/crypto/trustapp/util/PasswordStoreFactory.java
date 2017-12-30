package com.wallet.crypto.trustapp.util;

import android.content.Context;
import android.os.Build;

import com.wallet.crypto.trustapp.entity.ServiceErrorException;
import com.wallet.pwd.trustapp.PasswordManager;

import java.security.SecureRandom;

public class PasswordStoreFactory {
	public static void put(Context context, String address, String password) throws ServiceErrorException {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			KS.put(context, address, password);
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

	public static String generatePassword() {
		byte bytes[] = new byte[256];
		SecureRandom random = new SecureRandom();
		random.nextBytes(bytes);
		return String.valueOf(bytes);
	}
}
