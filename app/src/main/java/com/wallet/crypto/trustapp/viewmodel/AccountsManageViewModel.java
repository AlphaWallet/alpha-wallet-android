package com.wallet.crypto.trustapp.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;

import com.wallet.crypto.trustapp.entity.Account;
import com.wallet.crypto.trustapp.repository.AccountRepositoryType;
import com.wallet.crypto.trustapp.router.CreateAccountRouter;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class AccountsManageViewModel extends BaseViewModel {

	private final AccountRepositoryType accountRepository;
	private final CreateAccountRouter createAccountRouter;

	private final MutableLiveData<Account[]> accounts = new MutableLiveData<>();
	private final MutableLiveData<Account> defaultAccount = new MutableLiveData<>();


	AccountsManageViewModel(
			AccountRepositoryType accountRepository, CreateAccountRouter createAccountRouter) {
		this.accountRepository = accountRepository;
		this.createAccountRouter = createAccountRouter;

		fetchAccounts();
	}

	public LiveData<Account[]> accounts() {
		return accounts;
	}

	public LiveData<Account> defaultAccount() {
		return defaultAccount;
	}

	public void setDefaultAccount(Account account) {
		accountRepository
				.setCurrentAccount(account)
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(() -> {
					onDefaultAccountChanged(account);
				}, this::onError);
	}

	public void deleteAccount(Account account, String password) {
		accountRepository
				.deleteAccount(account.address, password)
				.andThen(accountRepository.fetchAccounts())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(this::onFetchAccounts, this::onError);
	}

	private void onFetchAccounts(Account[] items) {
		progress.postValue(false);
		accounts.postValue(items);
		accountRepository
				.getCurrentAccount()
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(this::onDefaultAccountChanged, this::onError);
	}

	private void onDefaultAccountChanged(Account account) {
		progress.postValue(false);
		defaultAccount.postValue(account);
	}

	public void fetchAccounts() {
		progress.postValue(true);
		accountRepository
				.fetchAccounts()
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(this::onFetchAccounts, this::onError);
	}

	public void createAccount(Context context) {
		createAccountRouter.open(context);
	}
}
