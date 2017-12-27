package com.wallet.crypto.trustapp.repository;

import android.content.Context;
import android.os.Build;

import com.wallet.crypto.trustapp.controller.ServiceErrorException;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.util.KS;
import com.wallet.pwd.trustapp.PasswordManager;

import java.util.UUID;

import io.reactivex.Completable;
import io.reactivex.Single;

public class TrustPasswordStore implements PasswordStore {

	private final Context context;

	public TrustPasswordStore(Context context) {
		this.context = context;
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
                KS.put(context, password, wallet.address);
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
		return Single.just(UUID.randomUUID().toString());
	}
}
