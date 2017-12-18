package com.wallet.crypto.trustapp.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;

import com.wallet.crypto.trustapp.entity.Account;
import com.wallet.crypto.trustapp.interact.CreateAccountInteract;
import com.wallet.crypto.trustapp.interact.DeleteAccountInteract;
import com.wallet.crypto.trustapp.interact.FetchAccountsInteract;
import com.wallet.crypto.trustapp.interact.FindDefaultAccountInteract;
import com.wallet.crypto.trustapp.interact.SetDefaultAccountInteract;
import com.wallet.crypto.trustapp.router.ImportAccountRouter;

public class AccountsManageViewModel extends BaseViewModel {

	private final CreateAccountInteract createAccountInteract;
	private final SetDefaultAccountInteract setDefaultAccountInteract;
	private final DeleteAccountInteract deleteAccountInteract;
	private final FetchAccountsInteract fetchAccountsInteract;
	private final FindDefaultAccountInteract findDefaultAccountInteract;

	private final ImportAccountRouter importAccountRouter;

	private final MutableLiveData<Account[]> accounts = new MutableLiveData<>();
	private final MutableLiveData<Account> defaultAccount = new MutableLiveData<>();

	AccountsManageViewModel(
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

		fetchAccounts();
	}

	public LiveData<Account[]> accounts() {
		return accounts;
	}

	public LiveData<Account> defaultAccount() {
		return defaultAccount;
	}

	public void setDefaultAccount(Account account) {
		disposable = setDefaultAccountInteract
				.set(account)
				.subscribe(() -> onDefaultAccountChanged(account), this::onError);
	}

	public void deleteAccount(Account account) {
		disposable = deleteAccountInteract
				.delete(account)
				.subscribe(this::onFetchAccounts, this::onError);
	}

	private void onFetchAccounts(Account[] items) {
		progress.postValue(false);
		accounts.postValue(items);
		disposable = findDefaultAccountInteract
				.find()
				.subscribe(this::onDefaultAccountChanged, this::onError);
	}

	private void onDefaultAccountChanged(Account account) {
		progress.postValue(false);
		defaultAccount.postValue(account);
	}

	public void fetchAccounts() {
		progress.postValue(true);
		disposable = fetchAccountsInteract
				.fetch()
				.subscribe(this::onFetchAccounts, this::onError);
	}

	public void newAccount() {
		progress.setValue(true);
		createAccountInteract
				.create()
				.subscribe(a -> fetchAccounts(), this::onError);
	}

	public void importAccount(Context context) {
		importAccountRouter.open(context);
	}
}
