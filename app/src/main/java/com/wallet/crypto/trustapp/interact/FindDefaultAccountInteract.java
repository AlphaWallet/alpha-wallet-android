package com.wallet.crypto.trustapp.interact;

import com.wallet.crypto.trustapp.entity.Account;
import com.wallet.crypto.trustapp.repository.AccountRepositoryType;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class FindDefaultAccountInteract {

	private final AccountRepositoryType accountRepository;

	public FindDefaultAccountInteract(AccountRepositoryType accountRepository) {
		this.accountRepository = accountRepository;
	}

	public Single<Account> find() {
		return accountRepository
				.getCurrentAccount()
				.observeOn(AndroidSchedulers.mainThread());
	}
}
