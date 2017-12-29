package com.wallet.crypto.trustapp.ui;

import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.entity.ErrorEnvelope;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.ui.widget.adapter.WalletsManageAdapter;
import com.wallet.crypto.trustapp.viewmodel.WalletsManageViewModel;
import com.wallet.crypto.trustapp.viewmodel.WalletsManageViewModelFactory;
import com.wallet.crypto.trustapp.widget.AddWalletView;
import com.wallet.crypto.trustapp.widget.BackupView;
import com.wallet.crypto.trustapp.widget.SystemView;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

import static com.wallet.crypto.trustapp.C.IMPORT_REQUEST_CODE;
import static com.wallet.crypto.trustapp.C.SHARE_REQUEST_CODE;

public class ManageWalletsActivity extends BaseActivity implements
		View.OnClickListener,
        AddWalletView.OnNewWalletClickListener,
        AddWalletView.OnImportWalletClickListener {

	@Inject
    WalletsManageViewModelFactory walletsManageViewModelFactory;
	WalletsManageViewModel viewModel;

	private WalletsManageAdapter adapter;

	private SystemView systemView;
	private Dialog dialog;
//	private View addAction;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		AndroidInjection.inject(this);
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_accounts);
		// Init toolbar
		toolbar();

		adapter = new WalletsManageAdapter(this::onSetWalletDefault, this::onDeleteWallet, this::onExportWallet);
		SwipeRefreshLayout refreshLayout = findViewById(R.id.refresh_layout);
		systemView = findViewById(R.id.system_view);

		RecyclerView list = findViewById(R.id.list);

		list.setLayoutManager(new LinearLayoutManager(this));
		list.setAdapter(adapter);

		systemView.attachRecyclerView(list);
		systemView.attachSwipeRefreshLayout(refreshLayout);

		viewModel = ViewModelProviders.of(this, walletsManageViewModelFactory)
				.get(WalletsManageViewModel.class);

		viewModel.error().observe(this, this::onError);
		viewModel.progress().observe(this, systemView::showProgress);
		viewModel.wallets().observe(this, this::onFetchWallet);
		viewModel.defaultWallet().observe(this, this::onChangeDefaultWallet);
		viewModel.createdWallet().observe(this, this::onCreatedWallet);
		viewModel.exportedStore().observe(this, this::openShareDialog);

		refreshLayout.setOnRefreshListener(viewModel::fetchWallets);
	}

    private void onExportWallet(Wallet wallet) {
        showBackupDialog(wallet, false);
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
			viewModel.showTransactions(this);
		} else {
			finish();
			System.exit(0);
		}
		// TODO: Process first start
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	    if (adapter.getItemCount() > 0) {
            getMenuInflater().inflate(R.menu.menu_add, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
            case R.id.action_add: {
                onAddWallet();
            } break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == IMPORT_REQUEST_CODE) {
		    if (resultCode == RESULT_OK) {
                viewModel.fetchWallets();
                Snackbar.make(systemView, getString(R.string.toast_message_wallet_imported), Snackbar.LENGTH_SHORT)
                        .show();
            }
		} else if (requestCode == SHARE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Snackbar.make(systemView, getString(R.string.toast_message_wallet_exported), Snackbar.LENGTH_SHORT)
                        .show();
            } else {
                dialog = buildDialog()
                        .setMessage(R.string.do_manage_make_backup)
                        .setPositiveButton(R.string.yes_continue, null)
                        .setNegativeButton(R.string.no_repeat,
                                (dialog, which) -> openShareDialog(viewModel.exportedStore().getValue()))
                        .create();
                dialog.show();
            }
        }
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.try_again: {
				viewModel.fetchWallets();
			} break;
		}
	}

	@Override
	public void onNewWallet(View view) {
		hideDialog();
		viewModel.newWallet();
	}

	@Override
	public void onImportWallet(View view) {
		hideDialog();
		viewModel.importWallet(this);
	}

	private void onAddWallet() {
		AddWalletView addWalletView = new AddWalletView(this);
		addWalletView.setOnNewWalletClickListener(this);
		addWalletView.setOnImportWalletClickListener(this);
		dialog = new BottomSheetDialog(this);
		dialog.setContentView(addWalletView);
		dialog.setCancelable(true);
		dialog.setCanceledOnTouchOutside(true);
		dialog.show();
	}

	private void onChangeDefaultWallet(Wallet wallet) {
		adapter.setDefaultWallet(wallet);
	}

	private void onFetchWallet(Wallet[] wallets) {
		if (wallets == null || wallets.length == 0) {
			dissableDisplayHomeAsUp();
			AddWalletView addWalletView = new AddWalletView(this, R.layout.layout_empty_add_account);
			addWalletView.setOnNewWalletClickListener(this);
			addWalletView.setOnImportWalletClickListener(this);
			systemView.showEmpty(addWalletView);
			adapter.setWallets(new Wallet[0]);
		} else {
			enableDisplayHomeAsUp();
			adapter.setWallets(wallets);
		}
		invalidateOptionsMenu();
	}

	private void onCreatedWallet(Wallet wallet) {
		dialog = buildDialog()
				.setTitle(R.string.message_no_backup)
				.setIcon(R.mipmap.backup_warning)
				.setMessage(R.string.backup_warning_text)
				.setPositiveButton(R.string.action_backup_wallet,
                        (dialogInterface, i) -> showBackupDialog(wallet, true))
				.setNegativeButton(R.string.action_backup_later,
                        (dialogInterface, i) -> showNoBackupWarning(wallet))
				.create();
		dialog.show();
	}

    private void showNoBackupWarning(Wallet wallet) {
        dialog = buildDialog()
                .setTitle(getString(R.string.title_dialog_watch_out))
                .setMessage(getString(R.string.dialog_message_unrecoverable_message))
                .setIcon(R.drawable.ic_warning_black_24dp)
                .setPositiveButton(android.R.string.yes,
                        (dialog, whichButton) -> showBackupDialog(wallet, true))
                .setNegativeButton(android.R.string.no, null)
                .create();
        dialog.show();
    }

    private void showBackupDialog(Wallet wallet, boolean isNew) {
	    BackupView view = new BackupView(this);
        dialog = buildDialog()
                .setView(view)
                .setPositiveButton(R.string.ok,
                        (dialogInterface, i) -> viewModel.exportWallet(wallet, view.getPassword()))
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                    if (isNew) {
                        onCreatedWallet(wallet);
                    }
                })
                .create();
        dialog.show();
    }

    private void openShareDialog(String jsonData) {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Keystore");
        sharingIntent.putExtra(Intent.EXTRA_TEXT, jsonData);
        startActivityForResult(
                Intent.createChooser(sharingIntent, "Share via"),
                SHARE_REQUEST_CODE);
    }

	private void onError(ErrorEnvelope errorEnvelope) {
		systemView.showError(errorEnvelope.message, this);
	}

	private void onSetWalletDefault(Wallet wallet) {
		viewModel.setDefaultWallet(wallet);
	}

	private void onDeleteWallet(Wallet wallet) {
		dialog = buildDialog()
				.setTitle(getString(R.string.title_delete_account))
				.setMessage(getString(R.string.confirm_delete_account))
				.setIcon(R.drawable.ic_warning_black_24dp)
				.setPositiveButton(android.R.string.yes, (dialog, btn) -> viewModel.deleteWallet(wallet))
				.setNegativeButton(android.R.string.no, null)
				.create();
		dialog.show();
	}

	private AlertDialog.Builder buildDialog() {
	    hideDialog();
	    return new AlertDialog.Builder(this);
    }

	private void hideDialog() {
		if (dialog != null && dialog.isShowing()) {
			dialog.hide();
			dialog = null;
		}
	}
}
