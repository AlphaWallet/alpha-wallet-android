package com.wallet.crypto.trustapp.interact;

import com.wallet.crypto.trustapp.entity.Account;
import com.wallet.crypto.trustapp.repository.AccountRepositoryType;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class FetchAccountsInteract {

	private final AccountRepositoryType accountRepository;

	public FetchAccountsInteract(AccountRepositoryType accountRepository) {
		this.accountRepository = accountRepository;
	}

	public Single<Account[]> fetch() {
		return accountRepository
				.fetchAccounts()
				.observeOn(AndroidSchedulers.mainThread());
	}
}
