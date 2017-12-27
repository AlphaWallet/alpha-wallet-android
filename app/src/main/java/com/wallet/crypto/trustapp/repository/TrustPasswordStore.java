package com.wallet.crypto.trustapp.repository;

import android.content.Context;

import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.util.KS;

import java.util.UUID;

import io.reactivex.Completable;
import io.reactivex.Single;

public class KSPasswordStore implements PasswordStore {

	private final Context context;

	public KSPasswordStore(Context context) {
		this.context = context;
	}

	@Override
	public Single<String> getPassword(Wallet wallet) {
		return Single.fromCallable(() -> new String(KS.get(context, wallet.address)));
	}

	@Override
	public Completable setPassword(Wallet wallet, String password) {
		return Completable.fromAction(() -> KS.put(context, wallet.address, password));
	}

	@Override
	public Single<String> generatePassword() {
		return Single.just(UUID.randomUUID().toString());
	}
}
