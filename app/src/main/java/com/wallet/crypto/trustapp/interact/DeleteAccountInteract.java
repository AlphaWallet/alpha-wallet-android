package com.wallet.crypto.trustapp.interact;

import com.wallet.crypto.trustapp.entity.Account;
import com.wallet.crypto.trustapp.repository.AccountRepositoryType;
import com.wallet.crypto.trustapp.repository.PasswordStore;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * Delete and fetch accounts
 */
public class DeleteAccountInteract {
	private final AccountRepositoryType accountRepository;
	private final PasswordStore passwordStore;

	public DeleteAccountInteract(AccountRepositoryType accountRepository, PasswordStore passwordStore) {
		this.accountRepository = accountRepository;
		this.passwordStore = passwordStore;
	}

	public Single<Account[]> delete(Account account) {
		return Single.fromCallable(() -> passwordStore.getPassword(account))
				.flatMapCompletable(password -> accountRepository.deleteAccount(account.address, password))
				.andThen(accountRepository.fetchAccounts())
				.observeOn(AndroidSchedulers.mainThread());
	}
}
