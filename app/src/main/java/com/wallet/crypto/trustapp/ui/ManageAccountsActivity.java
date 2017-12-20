package com.wallet.crypto.trustapp.ui;

import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.entity.Account;
import com.wallet.crypto.trustapp.entity.ErrorEnvelope;
import com.wallet.crypto.trustapp.ui.widget.adapter.AccountsManageAdapter;
import com.wallet.crypto.trustapp.viewmodel.AccountsManageViewModel;
import com.wallet.crypto.trustapp.viewmodel.AccountsManageViewModelFactory;
import com.wallet.crypto.trustapp.widget.AddAccountView;
import com.wallet.crypto.trustapp.widget.SystemView;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class ManageAccountsActivity extends BaseActivity implements
		View.OnClickListener,
		AddAccountView.OnNewAccountClickListener,
		AddAccountView.OnImportAccountClickListener {

	@Inject
	AccountsManageViewModelFactory accountsManageViewModelFactory;
	AccountsManageViewModel viewModel;

	private AccountsManageAdapter adapter;

	private SystemView systemView;
	private Dialog dialog;
	private View addAction;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		AndroidInjection.inject(this);
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_accounts);
		// Init toolbar
		toolbar();

		adapter = new AccountsManageAdapter(this::onSetAccountDefault, this::onDeleteAccount);
		SwipeRefreshLayout refreshLayout = findViewById(R.id.refresh_layout);
		systemView = findViewById(R.id.system_view);
		addAction = findViewById(R.id.fab);

		RecyclerView list = findViewById(R.id.list);

		list.setLayoutManager(new LinearLayoutManager(this));
		list.setAdapter(adapter);

		addAction.setOnClickListener(this);

		systemView.attachRecyclerView(list);
		systemView.attachSwipeRefreshLayout(refreshLayout);

		viewModel = ViewModelProviders.of(this, accountsManageViewModelFactory)
				.get(AccountsManageViewModel.class);

		viewModel.error().observe(this, this::onError);
		viewModel.progress().observe(this, systemView::showProgress);
		viewModel.accounts().observe(this, this::onFetchAccount);
		viewModel.defaultAccount().observe(this, this::onChangeDefaultAccount);
		viewModel.addedAccount().observe(this, this::onAddedAccount);
		viewModel.createdAccount().observe(this, this::onCreatedAccount);

		refreshLayout.setOnRefreshListener(viewModel::fetchAccounts);
	}

	@Override
	protected void onPause() {
		super.onPause();

		hideDialog();
	}

	@Override
	public void onBackPressed() {
		// User can't start work without wallet.
		if (adapter.getItemCount() > 0) {
			super.onBackPressed();
		} else {
			finish();
			System.exit(0);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
			case AccountsManageViewModel.EXPORT_REQUEST: {
//				Account account = data.getParcelableExtra(ExportAccountRouter.WALLET);
//				onAddedAccount(account); // TODO: Not now. Need more things
			} break;
			case AccountsManageViewModel.IMPORT_REQUEST: {
				if (resultCode == RESULT_OK) {
//					Account account = data.getParcelableExtra(ImportAccountRouter.);
//					onAddedAccount(account); // TODO: Not now. Need more things
				}
			} break;
		}
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.fab: {
				onAddAccount();
			} break;
			case R.id.try_again: {
				viewModel.fetchAccounts();
			} break;
		}
	}

	@Override
	public void onNewAccount(View view) {
		hideDialog();
		viewModel.newAccount();
	}

	@Override
	public void onImportAccount(View view) {
		hideDialog();
		viewModel.importAccount(this);
	}

	private void onAddAccount() {
		AddAccountView addAccountView = new AddAccountView(this);
		addAccountView.setOnNewAccountClickListener(this);
		addAccountView.setOnImportAccountClickListener(this);
		dialog = new BottomSheetDialog(this);
		dialog.setContentView(addAccountView);
		dialog.setCancelable(true);
		dialog.setCanceledOnTouchOutside(true);
		dialog.show();
	}

	private void onChangeDefaultAccount(Account account) {
		adapter.setDefaultAccount(account);
	}

	private void onFetchAccount(Account[] accounts) {
		if (accounts == null || accounts.length == 0) {
			dissableDisplayHomeAsUp();
			addAction.setVisibility(View.GONE);
			AddAccountView addAccountView = new AddAccountView(this, R.layout.layout_empty_add_account);
			addAccountView.setOnNewAccountClickListener(this);
			addAccountView.setOnImportAccountClickListener(this);
			systemView.showEmpty(addAccountView);
			adapter.setAccounts(new Account[0]);
		} else {
			enableDisplayHomeAsUp();
			addAction.setVisibility(View.VISIBLE);
			adapter.setAccounts(accounts);
		}
	}

	private void onCreatedAccount(Account account) {
		dialog = new AlertDialog.Builder(this)
				.setTitle(R.string.message_no_backup)
				.setIcon(R.mipmap.backup_warning)
				.setMessage(R.string.backup_warning_text)
				.setPositiveButton(R.string.action_backup_wallet, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						viewModel.backupAccount(ManageAccountsActivity.this, account);
					}
				})
				.setNegativeButton(R.string.action_backup_later, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						onAddedAccount(account);
					}
				})
				.create();
		dialog.show();
	}

	private void onAddedAccount(Account account) {
		dialog = new AlertDialog.Builder(this)
				.setMessage(R.string.set_account_default)
				.setPositiveButton(R.string.action_backup_wallet, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						viewModel.setDefaultAccount(account);
					}
				})
				.setNegativeButton(R.string.action_backup_later, null)
				.create();
		dialog.show();
	}

	private void onError(ErrorEnvelope errorEnvelope) {
		systemView.showError(errorEnvelope.message, this);
	}

	private void onSetAccountDefault(Account account) {
		viewModel.setDefaultAccount(account);
	}

	private void onDeleteAccount(Account account) {
		dialog = new AlertDialog.Builder(this)
				.setTitle(getString(R.string.title_delete_account))
				.setMessage(getString(R.string.confirm_delete_account))
				.setIcon(R.drawable.ic_warning_black_24dp)
				.setPositiveButton(android.R.string.yes, (dialog, btn) -> viewModel.deleteAccount(account))
				.setNegativeButton(android.R.string.no, null)
				.create();
		dialog.show();
	}

	private void hideDialog() {
		if (dialog != null && dialog.isShowing()) {
			dialog.hide();
			dialog = null;
		}
	}
}
