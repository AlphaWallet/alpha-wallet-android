package com.wallet.crypto.trustapp.repository;

import android.content.Context;

import com.wallet.crypto.trustapp.entity.Account;
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
	public Single<String> getPassword(Account account) {
		return Single.fromCallable(() -> new String(KS.get(context, account.address)));
	}

	@Override
	public Completable setPassword(Account account, String password) {
		return Completable.fromAction(() -> KS.put(context, account.address, password));
	}

	@Override
	public Single<String> generatePassword() {
		return Single.just(UUID.randomUUID().toString());
	}
}
