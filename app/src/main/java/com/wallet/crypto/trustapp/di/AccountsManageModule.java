package com.wallet.crypto.trustapp.di;

import com.wallet.crypto.trustapp.repository.AccountRepositoryType;
import com.wallet.crypto.trustapp.router.CreateAccountRouter;
import com.wallet.crypto.trustapp.viewmodel.AccountsManageViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class AccountsManageModule {

	@Provides
	AccountsManageViewModelFactory provideAccountsManageViewModelFactory(
			AccountRepositoryType accountRepository,
	        CreateAccountRouter createAccountRouter) {
		return new AccountsManageViewModelFactory(accountRepository, createAccountRouter);
	}

	@Provides
	CreateAccountRouter provideCreateAccountRouter() {
		return new CreateAccountRouter();
	}
}
