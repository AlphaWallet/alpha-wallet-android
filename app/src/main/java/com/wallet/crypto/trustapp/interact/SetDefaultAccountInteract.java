package com.wallet.crypto.trustapp.interact;

import com.wallet.crypto.trustapp.entity.Account;
import com.wallet.crypto.trustapp.repository.AccountRepositoryType;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class SetDefaultAccountInteract {

	private AccountRepositoryType accountRepository;

	public SetDefaultAccountInteract(AccountRepositoryType accountRepositoryType) {
		this.accountRepository = accountRepositoryType;
	}

	public Completable set(Account account) {
		return accountRepository
				.setCurrentAccount(account)
				.observeOn(AndroidSchedulers.mainThread());
	}
}
