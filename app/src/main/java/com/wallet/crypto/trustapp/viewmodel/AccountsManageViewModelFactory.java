package com.wallet.crypto.trustapp.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.wallet.crypto.trustapp.interact.CreateAccountInteract;
import com.wallet.crypto.trustapp.interact.DeleteAccountInteract;
import com.wallet.crypto.trustapp.interact.FetchAccountsInteract;
import com.wallet.crypto.trustapp.interact.FindDefaultAccountInteract;
import com.wallet.crypto.trustapp.interact.SetDefaultAccountInteract;
import com.wallet.crypto.trustapp.router.ImportAccountRouter;

import javax.inject.Inject;

public class AccountsManageViewModelFactory implements ViewModelProvider.Factory {

	private final CreateAccountInteract createAccountInteract;
	private final SetDefaultAccountInteract setDefaultAccountInteract;
	private final DeleteAccountInteract deleteAccountInteract;
	private final FetchAccountsInteract fetchAccountsInteract;
	private final FindDefaultAccountInteract findDefaultAccountInteract;

	private final ImportAccountRouter importAccountRouter;

	@Inject
	public AccountsManageViewModelFactory(
			CreateAccountInteract createAccountInteract,
			SetDefaultAccountInteract setDefaultAccountInteract,
			DeleteAccountInteract deleteAccountInteract,
			FetchAccountsInteract fetchAccountsInteract,
			FindDefaultAccountInteract findDefaultAccountInteract,
			ImportAccountRouter importAccountRouter) {
		this.createAccountInteract = createAccountInteract;
		this.setDefaultAccountInteract = setDefaultAccountInteract;
		this.deleteAccountInteract = deleteAccountInteract;
		this.fetchAccountsInteract = fetchAccountsInteract;
		this.findDefaultAccountInteract = findDefaultAccountInteract;
		this.importAccountRouter = importAccountRouter;
	}

	@NonNull
	@Override
	public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
		return (T) new AccountsManageViewModel(
				createAccountInteract,
				setDefaultAccountInteract,
				deleteAccountInteract,
				fetchAccountsInteract,
				findDefaultAccountInteract,
				importAccountRouter);
	}
}
