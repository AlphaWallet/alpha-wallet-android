package com.wallet.crypto.trustapp.interact;

import com.wallet.crypto.trustapp.entity.Account;
import com.wallet.crypto.trustapp.repository.AccountRepositoryType;
import com.wallet.crypto.trustapp.repository.PasswordStore;

import io.reactivex.Single;
import io.reactivex.observers.DisposableCompletableObserver;

public class CreateAccountInteract {

	private final AccountRepositoryType accountRepository;
	private final PasswordStore passwordStore;

	public CreateAccountInteract(AccountRepositoryType accountRepository, PasswordStore passwordStore) {
		this.accountRepository = accountRepository;
		this.passwordStore = passwordStore;
	}

	public Single<Account> create() {
		return passwordStore.generatePassword()
				.flatMap(password -> accountRepository
						.createAccount(password)
						.compose(createStream -> savePassword(createStream.blockingGet(), password)));
	}

	private Single<Account> savePassword(Account account, String password) {
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
						}))
				.toSingle(() -> account);
	}
}
