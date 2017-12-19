package com.wallet.crypto.trustapp.repository;

import com.wallet.crypto.trustapp.entity.Account;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface PasswordStore {
	Single<String> getPassword(Account account);
	Completable setPassword(Account account, String password);
	Single<String> generatePassword();
}
