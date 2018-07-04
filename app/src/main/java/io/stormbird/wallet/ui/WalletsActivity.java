package io.stormbird.wallet.ui;

import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.math.BigDecimal;
import java.util.Map;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.ErrorEnvelope;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.ui.widget.adapter.WalletsAdapter;
import io.stormbird.wallet.util.KeyboardUtils;
import io.stormbird.wallet.viewmodel.WalletsViewModel;
import io.stormbird.wallet.viewmodel.WalletsViewModelFactory;
import io.stormbird.wallet.widget.AWalletAlertDialog;
import io.stormbird.wallet.widget.AddWalletView;
import io.stormbird.wallet.widget.BackupView;
import io.stormbird.wallet.widget.BackupWarningView;
import io.stormbird.wallet.widget.SystemView;

import static io.stormbird.wallet.C.IMPORT_REQUEST_CODE;
import static io.stormbird.wallet.C.RESET_WALLET;
import static io.stormbird.wallet.C.SHARE_REQUEST_CODE;

public class WalletsActivity extends BaseActivity implements
        View.OnClickListener,
        AddWalletView.OnNewWalletClickListener,
        AddWalletView.OnImportWalletClickListener {

    @Inject
    WalletsViewModelFactory walletsViewModelFactory;
    WalletsViewModel viewModel;

    private WalletsAdapter adapter;

    private SystemView systemView;
    private BackupWarningView backupWarning;
    private Dialog dialog;
    private boolean isSetDefault;
    private final Handler handler = new Handler();
    private AWalletAlertDialog aDialog;
    private boolean walletChange = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wallets);
        // Init toolbar
        toolbar();
        setTitle(R.string.empty);

        adapter = new WalletsAdapter(this::onSetWalletDefault, this::onDeleteWallet, this::onExportWallet);
        SwipeRefreshLayout refreshLayout = findViewById(R.id.refresh_layout);
        systemView = findViewById(R.id.system_view);
        backupWarning = findViewById(R.id.backup_warning);

        RecyclerView list = findViewById(R.id.list);

        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        systemView.attachRecyclerView(list);
        systemView.attachSwipeRefreshLayout(refreshLayout);
        backupWarning.setOnPositiveClickListener(this::onNowBackup);
        backupWarning.setOnNegativeClickListener(this::onLaterBackup);

        viewModel = ViewModelProviders.of(this, walletsViewModelFactory)
                .get(WalletsViewModel.class);

        viewModel.error().observe(this, this::onError);
        viewModel.progress().observe(this, systemView::showProgress);
        viewModel.wallets().observe(this, this::onFetchWallet);
        viewModel.defaultWallet().observe(this, this::onChangeDefaultWallet);
        viewModel.createdWallet().observe(this, this::onCreatedWallet);
        viewModel.createWalletError().observe(this, this::onCreateWalletError);
        viewModel.exportedStore().observe(this, this::openShareDialog);
        viewModel.exportWalletError().observe(this, this::onExportWalletError);
        viewModel.deleteWalletError().observe(this, this::onDeleteWalletError);
        viewModel.updateBalance().observe(this, this::onUpdatedBalance);

        refreshLayout.setOnRefreshListener(viewModel::fetchWallets);
    }

    private void onCreateWalletError(ErrorEnvelope errorEnvelope) {
        aDialog = new AWalletAlertDialog(this);
        aDialog.setTitle(R.string.title_dialog_error);
        aDialog.setIcon(AWalletAlertDialog.ERROR);
        aDialog.setMessage(TextUtils.isEmpty(errorEnvelope.message)
                ? getString(R.string.error_create_wallet)
                : errorEnvelope.message);
        aDialog.setButtonText(R.string.dialog_ok);
        aDialog.setButtonListener(v -> aDialog.dismiss());
        aDialog.show();
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
            finish();
        } else {
            finish();
            System.exit(0);
        }
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
            }
            break;
            case android.R.id.home: {
                onBackPressed();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMPORT_REQUEST_CODE) {
            showToolbar();
            if (resultCode == RESULT_OK)
            {
                viewModel.fetchWallets();
                Snackbar.make(systemView, getString(R.string.toast_message_wallet_imported), Snackbar.LENGTH_SHORT)
                        .show();
                //set as isSetDefault
				Wallet importedWallet = data.getParcelableExtra(C.Key.WALLET);
				if (importedWallet != null)
				{
					viewModel.setDefaultWallet(importedWallet);
				}
                if (adapter.getItemCount() <= 1) {
                    viewModel.showHome(this);
                }

                sendBroadcast(new Intent(RESET_WALLET));
            }
        } else if (requestCode == SHARE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Snackbar.make(systemView, getString(R.string.toast_message_wallet_exported), Snackbar.LENGTH_SHORT)
                        .show();
                backupWarning.hide();
                showToolbar();
                hideDialog();
                if (adapter.getItemCount() <= 1) {
                    onBackPressed();
                }
            } else {
                aDialog = new AWalletAlertDialog(this);
                aDialog.setIcon(AWalletAlertDialog.NONE);
                aDialog.setTitle(R.string.do_manage_make_backup);
                aDialog.setButtonText(R.string.yes_continue);
                aDialog.setButtonListener(v -> {
                    hideDialog();
                    backupWarning.hide();
                    showToolbar();
                    if (adapter.getItemCount() <= 1) {
                        onBackPressed();
                    }
                });
                aDialog.setSecondaryButtonText(R.string.no_repeat);
                aDialog.setSecondaryButtonListener(v -> {
                    openShareDialog(viewModel.exportedStore().getValue());
                    hideDialog();
                });
                aDialog.show();
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.try_again: {
                viewModel.fetchWallets();
            }
            break;
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

    private void onUpdatedBalance(Map<String, BigDecimal> balances)
    {
        adapter.updateWalletBalances(balances);
    }

    private void onAddWallet() {
        AddWalletView addWalletView = new AddWalletView(this);
        addWalletView.setOnNewWalletClickListener(this);
        addWalletView.setOnImportWalletClickListener(this);
        dialog = new BottomSheetDialog(this);
        dialog.setContentView(addWalletView);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        BottomSheetBehavior behavior = BottomSheetBehavior.from((View) addWalletView.getParent());
        dialog.setOnShowListener(dialog -> behavior.setPeekHeight(addWalletView.getHeight()));
        dialog.show();
    }

    private void onChangeDefaultWallet(Wallet wallet) {
        if (walletChange)
        {
            walletChange = false;
            sendBroadcast(new Intent(RESET_WALLET));
        }

        if (isSetDefault) {
            viewModel.showHome(this);
        } else {
            adapter.setDefaultWallet(wallet);
        }
    }

    private void onFetchWallet(Wallet[] wallets) {
        if (wallets == null || wallets.length == 0) {
            dissableDisplayHomeAsUp();
            AddWalletView addWalletView = new AddWalletView(this, R.layout.layout_empty_add_account);
            addWalletView.setOnNewWalletClickListener(this);
            addWalletView.setOnImportWalletClickListener(this);
            systemView.showEmpty(addWalletView);
            adapter.setWallets(new Wallet[0]);
            hideToolbar();
        } else {
            enableDisplayHomeAsUp();
            adapter.setWallets(wallets);
        }
        invalidateOptionsMenu();
    }

    private void onCreatedWallet(Wallet wallet) {
        hideToolbar();
        //set new wallet
		viewModel.setDefaultWallet(wallet);
		isSetDefault = true;
        walletChange = true;
		//backupWarning.show(wallet);
    }

    private void onLaterBackup(View view, Wallet wallet) {
        showNoBackupWarning(wallet);
    }

    private void onNowBackup(View view, Wallet wallet) {
        showBackupDialog(wallet, true);
    }

    private void showNoBackupWarning(Wallet wallet) {
        aDialog = new AWalletAlertDialog(this);
        aDialog.setIcon(AWalletAlertDialog.WARNING);
        aDialog.setTitle(R.string.title_dialog_watch_out);
        aDialog.setMessage(R.string.dialog_message_unrecoverable_message);
        aDialog.setButtonText(R.string.i_understand);
        aDialog.setButtonListener(v -> {
            backupWarning.hide();
            showToolbar();
        });
        aDialog.setSecondaryButtonText(R.string.dialog_cancel_back);
        aDialog.show();
    }

    private void showBackupDialog(Wallet wallet, boolean isNew) {
        BackupView view = new BackupView(this);
        aDialog = new AWalletAlertDialog(this);
        aDialog.setIcon(AWalletAlertDialog.NONE);
        aDialog.setView(view);
        aDialog.setButtonText(R.string.dialog_ok);
        aDialog.setButtonListener(v -> {
            viewModel.exportWallet(wallet, view.getPassword());
            KeyboardUtils.hideKeyboard(view.findViewById(R.id.password));
            aDialog.dismiss();
        });
        aDialog.setSecondaryButtonText(R.string.dialog_cancel_back);
        aDialog.setSecondaryButtonListener(v -> {
            if (isNew) {
                onCreatedWallet(wallet);
            }
            KeyboardUtils.hideKeyboard(view.findViewById(R.id.password));
            aDialog.dismiss();
        });
        aDialog.setOnDismissListener(v -> KeyboardUtils.hideKeyboard(view.findViewById(R.id.password)));
        aDialog.show();
        handler.postDelayed(() -> KeyboardUtils.showKeyboard(view.findViewById(R.id.password)), 500);
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

    private void onExportWalletError(ErrorEnvelope errorEnvelope) {
        aDialog = new AWalletAlertDialog(this);
        aDialog.setTitle(R.string.title_dialog_error);
        aDialog.setIcon(AWalletAlertDialog.ERROR);
        aDialog.setMessage(TextUtils.isEmpty(errorEnvelope.message)
                ? getString(R.string.error_export)
                : errorEnvelope.message);
        aDialog.setButtonText(R.string.dialog_ok);
        aDialog.setButtonListener(v -> aDialog.dismiss());
        aDialog.show();
    }

    private void onDeleteWalletError(ErrorEnvelope errorEnvelope) {
        aDialog = new AWalletAlertDialog(this);
        aDialog.setTitle(R.string.title_dialog_error);
        aDialog.setIcon(AWalletAlertDialog.ERROR);
        aDialog.setMessage(TextUtils.isEmpty(errorEnvelope.message)
                ? getString(R.string.error_deleting_account)
                : errorEnvelope.message);
        aDialog.setButtonText(R.string.dialog_ok);
        aDialog.setButtonListener(v -> aDialog.dismiss());
        aDialog.show();
    }

    private void onError(ErrorEnvelope errorEnvelope) {
        systemView.showError(errorEnvelope.message, this);
    }

    private void onSetWalletDefault(Wallet wallet) {
        viewModel.setDefaultWallet(wallet);
        isSetDefault = true;
        walletChange = true;
    }

    private void onDeleteWallet(Wallet wallet) {
        aDialog = new AWalletAlertDialog(this);
        aDialog.setIcon(AWalletAlertDialog.WARNING);
        aDialog.setTitle(R.string.title_delete_account);
        aDialog.setMessage(R.string.confirm_delete_account);
        aDialog.setButtonText(R.string.dialog_ok);
        aDialog.setButtonListener(v -> {
            walletChange = true;
            viewModel.deleteWallet(wallet);
            aDialog.dismiss();
        });
        aDialog.setSecondaryButtonText(R.string.dialog_cancel_back);
        aDialog.show();
    }

    private void hideDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
            dialog = null;
        }

        if (aDialog != null && aDialog.isShowing()) {
            aDialog.dismiss();
            aDialog = null;
        }
    }
}
