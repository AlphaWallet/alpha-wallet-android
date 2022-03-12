package com.alphawallet.app.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import com.alphawallet.app.C;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.BackupOperationType;
import com.alphawallet.app.entity.BackupState;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.google.android.flexbox.FlexboxLayout;

import static com.alphawallet.app.C.Key.WALLET;
import static com.alphawallet.app.entity.BackupState.ENTER_BACKUP_STATE_HD;
import static com.alphawallet.app.entity.BackupState.ENTER_JSON_BACKUP;
import static com.alphawallet.app.entity.BackupState.UPGRADE_KEY_SECURITY;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class BackupFlowActivity extends BaseActivity implements
        View.OnClickListener,
        StandardFunctionInterface {

    private BackupState state;
    private Wallet wallet;
    private TextView title;
    private TextView detail;
    private FlexboxLayout layoutWordHolder;
    private ImageView backupImage;
    private AWalletAlertDialog alertDialog;
    private FunctionButtonBar functionButtonBar;
    private BackupOperationType type;
    private boolean launchedBackup;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        alertDialog = null;
        lockOrientation();
        launchedBackup = false;

        type = (BackupOperationType) getIntent().getSerializableExtra("TYPE");
        wallet = getIntent().getParcelableExtra(WALLET);
        if (type == null) type = BackupOperationType.UNDEFINED;

        toolbar();

        switch (type) {
            case UNDEFINED:
                state = BackupState.UNDEFINED;
                DisplayKeyFailureDialog("Unknown Key operation");
                break;
            case BACKUP_HD_KEY:
                state = ENTER_BACKUP_STATE_HD;
                setHDBackupSplash();
                break;
            case BACKUP_KEYSTORE_KEY:
                state = ENTER_JSON_BACKUP;
                setupJSONExport();
                break;
            case UPGRADE_KEY:
                handleClick("", 0);
                break;
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private void lockOrientation() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    private void setHDBackupSplash() {
        setContentView(R.layout.activity_backup);
        initViews();
        title.setText(R.string.backup_seed_phrase);
        backupImage.setImageResource(R.drawable.seed);
        detail.setText(R.string.backup_seed_phrase_detail);
        functionButtonBar.setPrimaryButtonText(R.string.action_back_up_my_wallet);
        functionButtonBar.setPrimaryButtonClickListener(this);
    }

    private void setupJSONExport() {
        setContentView(R.layout.activity_backup);
        initViews();
        title.setText(R.string.what_is_keystore_json);
        backupImage.setImageResource(R.drawable.ic_keystore);
        detail.setText(R.string.keystore_detail_text);
        state = ENTER_JSON_BACKUP;
        functionButtonBar.setPrimaryButtonText(R.string.export_keystore_json);
        functionButtonBar.setPrimaryButtonClickListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        //hide seed phrase and any visible words
        if (layoutWordHolder != null) layoutWordHolder.removeAllViews();

        if (state == null) return;

        switch (state) {
            case WRITE_DOWN_SEED_PHRASE:
            case SHOW_SEED_PHRASE:
                setHDBackupSplash(); //note, the OS calls onPause if user chooses to authenticate using PIN or password (takes them to the auth screen).
                break;

            case SEED_PHRASE_INVALID:
            case VERIFY_SEED_PHRASE:
                state = ENTER_BACKUP_STATE_HD; //reset view back to splash screen
                setHDBackupSplash();
                break;

            case SET_JSON_PASSWORD:
                setupJSONExport();
                break;

            case ENTER_JSON_BACKUP:
            case ENTER_BACKUP_STATE_HD:
            case UPGRADE_KEY_SECURITY:
                break;
        }
    }

    private void initViews() {
        title = findViewById(R.id.text_title);
        detail = findViewById(R.id.text_detail);
        layoutWordHolder = findViewById(R.id.layout_word_holder);
        backupImage = findViewById(R.id.backup_seed_image);
        functionButtonBar = findViewById(R.id.layoutButtons);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        toolbar();
        setTitle(getString(R.string.empty));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
        {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        setResult(RESULT_CANCELED, intent);
        intent.putExtra("Key", wallet.address);
        finish();
    }

    @Override
    public void onClick(View view) {
        // Passing an empty String as this class handles clicks based on state
        handleClick("", 0);
    }

    private void DisplayKeyFailureDialog(String message) {
        hideDialog();

        alertDialog = new AWalletAlertDialog(this);
        alertDialog.setIcon(AWalletAlertDialog.ERROR);
        alertDialog.setTitle(R.string.key_error);
        alertDialog.setMessage(message);
        alertDialog.setButtonText(R.string.action_continue);
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.setButtonListener(v -> {
            alertDialog.dismiss();
        });
        alertDialog.setOnCancelListener(v -> {
        });
        alertDialog.show();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (launchedBackup) //avoid orphaned view.
        {
            finish();
        }
    }

    private void finishBackupSuccess(Intent data) {
        setResult(RESULT_OK, data);
        finish();
    }

    public void cancelAuthentication()
    {
        Intent intent = new Intent();
        setResult(RESULT_CANCELED, intent);
        intent.putExtra("Key", wallet.address);
        finish();
    }

    private void hideDialog() {
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
            alertDialog = null;
        }
    }

    ActivityResultLauncher<Intent> handleBackupWallet = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK)
                {
                    finishBackupSuccess(result.getData());
                }
                finish();
            });

    @Override
    public void handleClick(String action, int id) {
        Intent intent = new Intent(this, BackupKeyActivity.class);
        intent.putExtra(WALLET, wallet);
        launchedBackup = true;

        switch (type)
        {
            case BACKUP_HD_KEY:
                intent.putExtra("STATE", ENTER_BACKUP_STATE_HD);
                break;
            case BACKUP_KEYSTORE_KEY:
                intent.putExtra("STATE", ENTER_JSON_BACKUP);
                break;
            case UPGRADE_KEY:
                intent.putExtra("STATE", UPGRADE_KEY_SECURITY);
                break;
        }

        handleBackupWallet.launch(intent);
    }
}
