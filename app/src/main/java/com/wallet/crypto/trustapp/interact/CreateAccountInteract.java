package com.wallet.crypto.trustapp.interact;

import com.wallet.crypto.trustapp.entity.Account;
import com.wallet.crypto.trustapp.repository.AccountRepositoryType;
import com.wallet.crypto.trustapp.repository.PasswordStore;

import io.reactivex.Completable;
import io.reactivex.observers.DisposableCompletableObserver;

public class CreateAccountInteract {

	private final AccountRepositoryType accountRepository;
	private final PasswordStore passwordStore;

	public CreateAccountInteract(AccountRepositoryType accountRepository, PasswordStore passwordStore) {
		this.accountRepository = accountRepository;
		this.passwordStore = passwordStore;
	}

	public Completable create() {
		return passwordStore.generatePassword()
				.flatMapCompletable(password -> accountRepository
						.createAccount(password)
						.flatMapCompletable(account -> savePassword(account, password)));
	}

	private Completable savePassword(Account account, String password) {
		return passwordStore
				.setPassword(account, password)
				.onErrorResumeNext(err -> accountRepository.deleteAccount(account.address, password)
						.lift(observer -> new DisposableCompletableObserver() {
							@Override
							public void onComplete() {
								observer.onError(err);
							}

							@Override
							public void onError(Throwable e) {
								observer.onError(e);
							}
						}));
	}
}
