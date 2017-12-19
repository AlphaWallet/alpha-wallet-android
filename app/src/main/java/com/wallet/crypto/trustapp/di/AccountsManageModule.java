package com.wallet.crypto.trustapp.di;

import com.wallet.crypto.trustapp.interact.CreateAccountInteract;
import com.wallet.crypto.trustapp.interact.DeleteAccountInteract;
import com.wallet.crypto.trustapp.interact.FetchAccountsInteract;
import com.wallet.crypto.trustapp.interact.FindDefaultAccountInteract;
import com.wallet.crypto.trustapp.interact.SetDefaultAccountInteract;
import com.wallet.crypto.trustapp.repository.AccountRepositoryType;
import com.wallet.crypto.trustapp.repository.PasswordStore;
import com.wallet.crypto.trustapp.router.ImportAccountRouter;
import com.wallet.crypto.trustapp.viewmodel.AccountsManageViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class AccountsManageModule {

	@Provides
	AccountsManageViewModelFactory provideAccountsManageViewModelFactory(
			CreateAccountInteract createAccountInteract,
			SetDefaultAccountInteract setDefaultAccountInteract,
			DeleteAccountInteract deleteAccountInteract,
			FetchAccountsInteract fetchAccountsInteract,
			FindDefaultAccountInteract findDefaultAccountInteract,
			ImportAccountRouter importAccountRouter) {
		return new AccountsManageViewModelFactory(createAccountInteract,
				setDefaultAccountInteract,
				deleteAccountInteract,
				fetchAccountsInteract,
				findDefaultAccountInteract,
				importAccountRouter);
	}

	@Provides
	CreateAccountInteract provideCreateAccountInteract(
			AccountRepositoryType accountRepository, PasswordStore passwordStore) {
		return new CreateAccountInteract(accountRepository, passwordStore);
	}

	@Provides
	SetDefaultAccountInteract provideSetDefaultAccountInteract(AccountRepositoryType accountRepository) {
		return new SetDefaultAccountInteract(accountRepository);
	}

	@Provides
	DeleteAccountInteract provideDeleteAccountInteract(
			AccountRepositoryType accountRepository, PasswordStore store) {
		return new DeleteAccountInteract(accountRepository, store);
	}

	@Provides
	FetchAccountsInteract provideFetchAccountsInteract(AccountRepositoryType accountRepository) {
		return new FetchAccountsInteract(accountRepository);
	}

	@Provides
	FindDefaultAccountInteract provideFindDefaultAccountInteract(AccountRepositoryType accountRepository) {
		return new FindDefaultAccountInteract(accountRepository);
	}

	@Provides
	ImportAccountRouter provideImportAccountRouter() {
		return new ImportAccountRouter();
	}
}
