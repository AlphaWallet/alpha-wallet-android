package com.alphawallet.app.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.ErrorEnvelope;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.viewmodel.WalletActionsViewModel;
import com.alphawallet.app.viewmodel.WalletActionsViewModelFactory;
import com.alphawallet.app.widget.AWalletAlertDialog;

import static com.alphawallet.app.C.*;
import static com.alphawallet.app.C.Key.WALLET;

public class WalletActionsActivity extends BaseActivity implements View.OnClickListener, Runnable {
    @Inject
    WalletActionsViewModelFactory walletActionsViewModelFactory;
    WalletActionsViewModel viewModel;

    private TextView walletTitle;
    private TextView walletBalance;
    private TextView walletAddress;
    private LinearLayout successOverlay;
    private Button save;
    private EditText walletName;
    private TextView delete;
    private TextView backUp;
    private AWalletAlertDialog aDialog;
    private final Handler handler = new Handler();

    private Wallet wallet;
    private String currencySymbol;
    private int walletCount;
    private boolean isNewWallet;
    private Boolean isTaskRunning;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_actions);
        toolbar();
        setTitle(getString(R.string.manage_wallet));

        if (getIntent() != null) {
            wallet = (Wallet) getIntent().getExtras().get("wallet");
            currencySymbol = getIntent().getStringExtra("currency");
            walletCount = getIntent().getIntExtra("walletCount", 0);
            walletCount++;
            isNewWallet = getIntent().getBooleanExtra("isNewWallet", false);
            initViews();
        } else {
            finish();
        }

        initViewModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        successOverlay = findViewById(R.id.layout_success_overlay);
    }

    private void initViewModel() {
        viewModel = ViewModelProviders.of(this, walletActionsViewModelFactory)
                .get(WalletActionsViewModel.class);

        viewModel.saved().observe(this, this::onSaved);
        viewModel.deleteWalletError().observe(this, this::onDeleteError);
        viewModel.exportWalletError().observe(this, this::onExportError);
        viewModel.deleted().observe(this, this::onDeleteWallet);
        viewModel.exportedStore().observe(this, this::onBackupWallet);
        viewModel.isTaskRunning().observe(this, this::onTaskStatusChanged);

        if (isNewWallet) {
            wallet.name = getString(R.string.wallet_name_template, walletCount);
            viewModel.storeWallet(wallet);
        }
    }

    private void onTaskStatusChanged(Boolean isTaskRunning) {
        this.isTaskRunning = isTaskRunning;
    }

    private void onSaved(Integer integer) {
        if (!isNewWallet) {
            showWalletsActivity();
        }
    }

    private void onBackupWallet(String keystore) {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Keystore");
        sharingIntent.putExtra(Intent.EXTRA_TEXT, keystore);
        startActivityForResult(
                Intent.createChooser(sharingIntent, "Share via"),
                SHARE_REQUEST_CODE);
    }

    private void onDeleteWallet(Boolean isDeleted) {
        if (isDeleted) {
            showWalletsActivity();
        }
    }

    private void onExportError(ErrorEnvelope errorEnvelope) {
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

    private void onDeleteError(ErrorEnvelope errorEnvelope) {
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

    private void initViews() {
        walletTitle = findViewById(R.id.wallet_title);

        walletBalance = findViewById(R.id.wallet_balance);
        walletBalance.setText(String.format("%s %s", wallet.balance, currencySymbol));

        walletAddress = findViewById(R.id.wallet_address);
        walletAddress.setText(wallet.address);

        save = findViewById(R.id.button_save);
        save.setOnClickListener(this);

        TextWatcher walletNameWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                save.setEnabled(!wallet.name.equals(editable.toString()));
            }
        };

        walletName = findViewById(R.id.wallet_name);
        if (wallet.name.isEmpty()) {
            enableDisplayHomeAsHome(false);
            walletName.setText(getString(R.string.wallet_name_template, walletCount));
            save.setEnabled(true);
        } else {
            walletName.setText(wallet.name);
        }

        walletName.addTextChangedListener(walletNameWatcher);

        delete = findViewById(R.id.delete);
        delete.setOnClickListener(this);

        backUp = findViewById(R.id.backup);
        backUp.setOnClickListener(this);

        if (wallet.type == WalletType.KEYSTORE)
        {
            backUp.setText(R.string.export_keystore_json);
            TextView backupDetail = findViewById(R.id.backup_text);
            backupDetail.setText(R.string.export_keystore_detail);
        }
        else if (wallet.type == WalletType.WATCH)
        {
            findViewById(R.id.layout_backup_method).setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.delete: {
                confirmDelete(wallet);
                break;
            }
            case R.id.backup: {
                if (wallet.type == WalletType.HDKEY)
                {
                    testSeedPhrase(wallet);
                }
                else
                {
                    exportJSON(wallet);
                    //startExport(wallet);
                }
                break;
            }
            case R.id.button_save: {
                wallet.name = walletName.getText().toString();
                viewModel.storeWallet(wallet);
                save.setEnabled(false);
                if (isNewWallet)
                {
                    viewModel.showHome(this);
                    finish(); //drop back to home screen, no need to recreate everything
                }
                break;
            }
        }
    }

    private void testSeedPhrase(Wallet wallet)
    {
        Intent intent = new Intent(this, BackupKeyActivity.class);
        intent.putExtra(WALLET, wallet);
        intent.putExtra("TYPE", BackupKeyActivity.BackupOperationType.SHOW_SEED_PHRASE);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        startActivityForResult(intent, C.REQUEST_BACKUP_WALLET);
    }

    private void showWalletsActivity()
    {
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
                break;
            }
        }
        return false;
    }

    private void confirmDelete(Wallet wallet) {
        aDialog = new AWalletAlertDialog(this);
        aDialog.setIcon(AWalletAlertDialog.WARNING);
        aDialog.setTitle(R.string.title_delete_account);
        aDialog.setMessage(R.string.confirm_delete_account);
        aDialog.setButtonText(R.string.dialog_ok);
        aDialog.setButtonListener(v -> {
            viewModel.deleteWallet(wallet);
            aDialog.dismiss();
        });
        aDialog.setSecondaryButtonText(R.string.dialog_cancel_back);
        aDialog.show();
    }

    private void exportJSON(Wallet wallet)
    {
        Intent intent = new Intent(this, BackupKeyActivity.class);
        intent.putExtra(WALLET, wallet);
        intent.putExtra("TYPE", BackupKeyActivity.BackupOperationType.BACKUP_KEYSTORE_KEY);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        startActivityForResult(intent, C.REQUEST_BACKUP_WALLET);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SHARE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, R.string.toast_message_wallet_exported, Toast.LENGTH_SHORT).show();
                showToolbar();
                hideDialog();
                backupSuccessful();
            } else {
                aDialog = new AWalletAlertDialog(this);
                aDialog.setIcon(AWalletAlertDialog.NONE);
                aDialog.setTitle(R.string.do_manage_make_backup);
                aDialog.setButtonText(R.string.yes_continue);
                aDialog.setButtonListener(v -> {
                    backupSuccessful();
                    hideDialog();
                    showToolbar();
                });
                aDialog.setSecondaryButtonText(R.string.no_repeat);
                aDialog.setSecondaryButtonListener(v -> {
                    onBackupWallet(viewModel.exportedStore().getValue());
                    hideDialog();
                });
                aDialog.show();
            }
        }
        else if (requestCode == C.REQUEST_BACKUP_WALLET && resultCode == RESULT_OK)
        {
            successOverlay.setVisibility(View.VISIBLE);
            handler.postDelayed(this, 1000);
            backupSuccessful();
        }
    }

    private void backupSuccessful()
    {
        Intent intent = new Intent(BACKUP_WALLET_SUCCESS);
        intent.putExtra("Key", wallet.address);
        sendBroadcast(intent);
    }

    private void hideDialog() {
        if (aDialog != null && aDialog.isShowing()) {
            aDialog.dismiss();
            aDialog = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (isNewWallet) {
            viewModel.showHome(this);
        } else {
            finish();
        }
    }

    @Override
    public void run()
    {
        if (successOverlay.getAlpha() > 0)
        {
            successOverlay.animate().alpha(0.0f).setDuration(500);
            handler.postDelayed(this, 750);
        }
        else
        {
            successOverlay.setVisibility(View.GONE);
            successOverlay.setAlpha(1.0f);
        }
    }
}
