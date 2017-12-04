package com.wallet.crypto.trustapp.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

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

public class AccountsManageActivity extends BaseActivity implements View.OnClickListener {

	@Inject
	AccountsManageViewModelFactory accountsManageViewModelFactory;
	AccountsManageViewModel viewModel;

	private AccountsManageAdapter adapter;

	private SystemView systemView;
	private AlertDialog dialog;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		AndroidInjection.inject(this);
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_accounts);

		toolbar();

		adapter = new AccountsManageAdapter(this::onSetAccountDefault, this::onDeleteAccount);
		systemView = findViewById(R.id.system_view);
		FloatingActionButton addAction = findViewById(R.id.fab);
		RecyclerView list = findViewById(R.id.list);
		list.setLayoutManager(new LinearLayoutManager(this));
		list.setAdapter(adapter);

		viewModel = ViewModelProviders.of(this, accountsManageViewModelFactory)
				.get(AccountsManageViewModel.class);

		viewModel.error().observe(this, this::onError);
		viewModel.progress().observe(this, this::onProgress);
		viewModel.accounts().observe(this, this::onFetchAccount);
		viewModel.defaultAccount().observe(this, this::onChangeDefaultAccount);

		addAction.setOnClickListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (dialog != null && dialog.isShowing()) {
			dialog.dismiss();
		}
	}

	private void onAddAccount() {
		viewModel.createAccount(this);
	}

	private void onChangeDefaultAccount(Account account) {
		adapter.setDefaultAccount(account);
	}

	private void onFetchAccount(Account[] accounts) {
		if (accounts == null || accounts.length == 0) {
			finish(); // Show create account screen // TODO: Bad solution. More good start create account activity with special flag.
		} else {
			adapter.setAccounts(accounts);
		}
	}

	private void onProgress(Boolean shouldShowProgress) {
		systemView.showProgress(shouldShowProgress);
	}

	private void onError(ErrorEnvelope errorEnvelope) {
		if (adapter.getItemCount() == 0) {
			systemView.showError(errorEnvelope.message, this);
		} else {
			Toast.makeText(getApplicationContext(), errorEnvelope.message, Toast.LENGTH_SHORT)
					.show();
		}
	}

	private void onTryAgain() {
		viewModel.fetchAccounts();
	}

	private void onSetAccountDefault(Account account) {
		viewModel.setDefaultAccount(account);
	}

	private void onDeleteAccount(Account account) {
		dialog = new AlertDialog.Builder(this)
			.setTitle(getString(R.string.title_delete_account))
			.setMessage(getString(R.string.confirm_delete_account))
			.setIcon(R.drawable.ic_warning_black_24dp)
			.setPositiveButton(android.R.string.yes, (dialog, btn) -> {
				try {
					viewModel.deleteAccount(
							account,
							PasswordManager.getPassword(account.address, getApplicationContext()));
					// todo: reload account list in controller.
				} catch (Exception ex) {
					String errorMessage = getString(
							R.string.error_deleting_account_with_details, ex.getLocalizedMessage());
					onError(new ErrorEnvelope(
							C.ErrorCode.CANT_GET_STORE_PASSWORD, errorMessage));
					ex.printStackTrace();
				}
			})
			.setNegativeButton(android.R.string.no, null)
			.create();
		dialog.show();
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.fab: {
				onAddAccount();
			} break;
			case R.id.try_again: {
				onTryAgain();
			} break;
		}
	}
}
