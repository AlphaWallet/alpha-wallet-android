package com.wallet.crypto.trustapp.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.wallet.crypto.trustapp.entity.ServiceErrorException;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.util.KS;
import com.wallet.pwd.trustapp.PasswordManager;

import java.security.SecureRandom;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Single;

public class TrustPasswordStore implements PasswordStore {

	private final Context context;

	public TrustPasswordStore(Context context) {
		this.context = context;

		migrate();
	}

    private void migrate() {
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
                } catch (Exception ex) {
                    Toast.makeText(context, "Could not process passwords.", Toast.LENGTH_LONG)
                            .show();
                    ex.printStackTrace();
                }
            }
        }
    }

    @Override
	public Single<String> getPassword(Wallet wallet) {
		return Single.fromCallable(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return new String(KS.get(context, wallet.address));
            } else {
                try {
                    return PasswordManager.getPassword(wallet.address, context);
                } catch (Exception e) {
                    throw new ServiceErrorException(ServiceErrorException.KEY_STORE_ERROR);
                }
            }
        });
	}

	@Override
	public Completable setPassword(Wallet wallet, String password) {
		return Completable.fromAction(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                KS.put(context, wallet.address, password);
            } else {
                try {
                    PasswordManager.setPassword(wallet.address, password, context);
                } catch (Exception e) {
                    throw new ServiceErrorException(ServiceErrorException.KEY_STORE_ERROR);
                }
            }
        });
	}

	@Override
	public Single<String> generatePassword() {
		return Single.fromCallable(() -> {
            byte bytes[] = new byte[256];
            SecureRandom random = new SecureRandom();
            random.nextBytes(bytes);
            return new String(bytes);
        });
	}
}
