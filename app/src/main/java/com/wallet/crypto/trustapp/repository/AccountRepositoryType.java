package com.wallet.crypto.trustapp.repository;

import com.wallet.crypto.trustapp.entity.Account;

import java.math.BigInteger;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;

public interface AccountRepositoryType {
	Single<Account[]> fetchAccounts();
	Maybe<Account> findAccount(String address);

	Single<Account> createAccount(String password);
	Single<Account> importAccount(String store, String password);
	Single<String> exportAccount(Account account, String password, String newPassword);

	Completable deleteAccount(String address, String password);

	Completable setCurrentAccount(Account account);
	Single<Account> getCurrentAccount();

	Maybe<BigInteger> ballanceInWei(Account account);
}
