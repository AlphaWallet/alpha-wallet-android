package com.wallet.crypto.trustapp.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.wallet.crypto.trustapp.C;
import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.controller.PasswordManager;
import com.wallet.crypto.trustapp.entity.Account;
import com.wallet.crypto.trustapp.entity.ErrorEnvelope;
import com.wallet.crypto.trustapp.ui.widget.adapter.AccountsManageAdapter;
import com.wallet.crypto.trustapp.viewmodel.AccountsManageViewModel;
import com.wallet.crypto.trustapp.viewmodel.AccountsManageViewModelFactory;
import com.wallet.crypto.trustapp.widget.SystemView;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class AccountsManageActivity extends BaseActivity {

	@Inject
	AccountsManageViewModelFactory accountsManageViewModelFactory;
	AccountsManageViewModel viewModel;

	private AccountsManageAdapter adapter;

	private RecyclerView list;
	private SystemView systemView;
	private FloatingActionButton addAction;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		AndroidInjection.inject(this);
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_accounts);

		toolbar();

		adapter = new AccountsManageAdapter(this::onSetAccountDefault, this::onDeleteAccount);
		systemView = findViewById(R.id.system_view);
		addAction = findViewById(R.id.fab);
		list = findViewById(R.id.list);
		list.setLayoutManager(new LinearLayoutManager(this));
		list.setAdapter(adapter);

		viewModel = ViewModelProviders.of(this, accountsManageViewModelFactory)
				.get(AccountsManageViewModel.class);

		viewModel.error().observe(this, this::onError);
		viewModel.progress().observe(this, this::onProgress);
		viewModel.accounts().observe(this, this::onFetchAccount);
		viewModel.defaultAccount().observe(this, this::onChangeDefaultAccount);

		addAction.setOnClickListener(this::onAddAccount);
	}

	private void onAddAccount(View view) {
		viewModel.createAccount(this);
	}

	private void onChangeDefaultAccount(Account account) {
		adapter.setDefaultAccount(account);
	}

	private void onFetchAccount(Account[] accounts) {
		adapter.setAccounts(accounts);
	}

	private void onProgress(Boolean shouldShowProgress) {
		systemView.showProgress(shouldShowProgress);
	}

	private void onError(ErrorEnvelope errorEnvelope) {
		systemView.showError(errorEnvelope.message, this::onTryAgain);
	}

	private void onTryAgain(View view) {
		viewModel.fetchAccounts();
	}

	private void onSetAccountDefault(Account account) {
		viewModel.setDefaultAccount(account);
	}

	private void onDeleteAccount(Account account) {
		try {
			viewModel.deleteAccount(account, PasswordManager.getPassword(account.address, this));
		} catch (Exception ex) {
			onError(new ErrorEnvelope(C.ErrorCode.CANT_GET_STORE_PASSWORD, null));
			ex.printStackTrace();
		}
	}
}
