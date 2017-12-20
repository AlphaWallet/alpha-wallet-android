package com.wallet.crypto.trustapp.viewmodel;

import android.app.Activity;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import com.wallet.crypto.trustapp.entity.Account;
import com.wallet.crypto.trustapp.interact.CreateAccountInteract;
import com.wallet.crypto.trustapp.interact.DeleteAccountInteract;
import com.wallet.crypto.trustapp.interact.FetchAccountsInteract;
import com.wallet.crypto.trustapp.interact.FindDefaultAccountInteract;
import com.wallet.crypto.trustapp.interact.SetDefaultAccountInteract;
import com.wallet.crypto.trustapp.router.ExportAccountRouter;
import com.wallet.crypto.trustapp.router.ImportAccountRouter;

public class AccountsManageViewModel extends BaseViewModel {

	public static final int IMPORT_REQUEST = 1001;
	public static final int EXPORT_REQUEST = 1002;

	private final CreateAccountInteract createAccountInteract;
	private final SetDefaultAccountInteract setDefaultAccountInteract;
	private final DeleteAccountInteract deleteAccountInteract;
	private final FetchAccountsInteract fetchAccountsInteract;
	private final FindDefaultAccountInteract findDefaultAccountInteract;

	private final ImportAccountRouter importAccountRouter;
	private final ExportAccountRouter exportAccountRouter;

	private final MutableLiveData<Account[]> accounts = new MutableLiveData<>();
	private final MutableLiveData<Account> defaultAccount = new MutableLiveData<>();
	private final MutableLiveData<Account> createdAccount = new MutableLiveData<>();
	private final MutableLiveData<Account> addedAccount = new MutableLiveData<>();

	AccountsManageViewModel(
			CreateAccountInteract createAccountInteract,
			SetDefaultAccountInteract setDefaultAccountInteract,
			DeleteAccountInteract deleteAccountInteract,
			FetchAccountsInteract fetchAccountsInteract,
			FindDefaultAccountInteract findDefaultAccountInteract,
			ImportAccountRouter importAccountRouter,
			ExportAccountRouter exportAccountRouter) {
		this.createAccountInteract = createAccountInteract;
		this.setDefaultAccountInteract = setDefaultAccountInteract;
		this.deleteAccountInteract = deleteAccountInteract;
		this.fetchAccountsInteract = fetchAccountsInteract;
		this.findDefaultAccountInteract = findDefaultAccountInteract;
		this.importAccountRouter = importAccountRouter;
		this.exportAccountRouter = exportAccountRouter;

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
				.subscribe(this::onDefaultAccountChanged, t -> {});
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
				.subscribe(account -> {
					fetchAccounts();
					createdAccount.postValue(account);
				}, this::onNewAccountError);
	}

	public LiveData<Account> createdAccount() {
		return createdAccount;
	}

	public LiveData<Account> addedAccount() {
		return addedAccount;
	}

	private void onNewAccountError(Throwable throwable) {

	}

	public void backupAccount(Activity activity, Account account) {
		exportAccountRouter.openForResult(activity, account, EXPORT_REQUEST);
	}

	public void importAccount(Activity activity) {
		importAccountRouter.openForResult(activity, IMPORT_REQUEST);
	}
}
