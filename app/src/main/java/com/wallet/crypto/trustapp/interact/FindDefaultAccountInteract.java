package com.wallet.crypto.trustapp.interact;

import com.wallet.crypto.trustapp.entity.Account;
import com.wallet.crypto.trustapp.repository.AccountRepositoryType;

import java.util.concurrent.Callable;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;

public class FindDefaultAccountInteract {

	private final AccountRepositoryType accountRepository;

	public FindDefaultAccountInteract(AccountRepositoryType accountRepository) {
		this.accountRepository = accountRepository;
	}

	public Single<Account> find() {
		return accountRepository
				.getCurrentAccount()
				.onErrorResumeNext(accountRepository
						.fetchAccounts()
						.to(single -> Flowable.fromArray(single.blockingGet()))
						.firstOrError()
						.flatMapCompletable(accountRepository::setCurrentAccount)
						.andThen(accountRepository.getCurrentAccount()))
				.observeOn(AndroidSchedulers.mainThread());
	}
}
