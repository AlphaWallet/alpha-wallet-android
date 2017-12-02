package com.wallet.crypto.trustapp.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.wallet.crypto.trustapp.repository.AccountRepositoryType;
import com.wallet.crypto.trustapp.router.CreateAccountRouter;

public class AccountsManageViewModelFactory implements ViewModelProvider.Factory {

	private final AccountRepositoryType accountRepository;
	private final CreateAccountRouter createAccountRouter;

	public AccountsManageViewModelFactory(
			AccountRepositoryType accountRepository,
			CreateAccountRouter createAccountRouter) {
		this.accountRepository = accountRepository;
		this.createAccountRouter = createAccountRouter;
	}

	@NonNull
	@Override
	public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
		return (T) new AccountsManageViewModel(accountRepository, createAccountRouter);
	}
}
