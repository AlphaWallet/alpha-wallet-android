package com.wallet.crypto.trustapp.interact;

import com.wallet.crypto.trustapp.entity.Account;
import com.wallet.crypto.trustapp.repository.AccountRepositoryType;

import java.util.UUID;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class CreateAccountInteract {

	private final AccountRepositoryType accountRepository;

	public CreateAccountInteract(AccountRepositoryType accountRepository) {
		this.accountRepository = accountRepository;
	}

	public Single<Account> create() {
		return accountRepository
				.createAccount(UUID.randomUUID().toString())
				.observeOn(AndroidSchedulers.mainThread());
	}
}
