package com.alphawallet.app.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.R;
import com.alphawallet.app.entity.BackupOperationType;
import com.alphawallet.app.entity.BackupState;
import com.alphawallet.app.entity.CreateWalletCallbackInterface;
import com.alphawallet.app.entity.Operation;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.StandardFunctionInterface;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.ui.QRScanning.DisplayUtils;
import com.alphawallet.app.util.Utils;
import com.alphawallet.app.viewmodel.BackupKeyViewModel;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.FunctionButtonBar;
import com.alphawallet.app.widget.LayoutCallbackListener;
import com.alphawallet.app.widget.PasswordInputView;
import com.alphawallet.app.widget.SignTransactionDialog;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

import static com.alphawallet.app.C.Key.WALLET;

@AndroidEntryPoint
public class BackupKeyActivity extends BaseActivity implements
        View.OnClickListener,
        CreateWalletCallbackInterface,
        TextWatcher,
        SignAuthenticationCallback,
        Runnable,
        LayoutCallbackListener,
        StandardFunctionInterface {

    BackupKeyViewModel viewModel;

    private BackupState state;
    private Wallet wallet;
    private TextView title;
    private TextView detail;
    private FlexboxLayout layoutWordHolder;
    private PasswordInputView inputView;
    private ImageView backupImage;
    private TextView verifyTextBox;
    private String[] mnemonicArray;
    private LinearLayout successOverlay;
    private final Handler handler = new Handler();
    private AWalletAlertDialog alertDialog;
    private String keystorePassword;
    private FunctionButtonBar functionButtonBar;

    private boolean hasNoLock = false;

    private int screenWidth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        secureWindow();

        alertDialog = null;
        lockOrientation();

        toolbar();
        initViewModel();

        screenWidth = DisplayUtils.getScreenResolution(this).x;
        wallet = getIntent().getParcelableExtra(WALLET);

        if (Objects.requireNonNull(getIntent().getExtras()).containsKey("STATE"))
        {
            initBackupState();
        }
        else
        {
            initBackupType();
        }
    }

    private void initBackupState() {
        state = (BackupState) getIntent().getSerializableExtra("STATE");

        assert state != null;
        switch (state) {
            case SHOW_SEED_PHRASE_SINGLE:
                showSeedPhrase();
                break;
            case ENTER_BACKUP_STATE_HD:
                WriteDownSeedPhrase();
                DisplaySeed();
                break;
            case WRITE_DOWN_SEED_PHRASE:
            case SHOW_SEED_PHRASE:
                VerifySeedPhrase();
                break;
            case VERIFY_SEED_PHRASE:
                TestSeedPhrase();
                break;
            case SEED_PHRASE_INVALID:
                ResetInputBox();
                VerifySeedPhrase();
                break;
            case ENTER_JSON_BACKUP:
                JSONBackup();
                break;
            case UPGRADE_KEY_SECURITY:
                //first open authentication
                setupUpgradeKey(false);
                break;
        }
    }

    private void showSeedPhrase() {
        setupTestSeed();
        ((TextView)findViewById(R.id.text_title)).setText(R.string.your_seed_phrase);
        DisplaySeed();
        functionButtonBar.setPrimaryButtonText(R.string.hide_seed_text);
        functionButtonBar.setPrimaryButtonClickListener(this);
    }

    private void initBackupType() {
        BackupOperationType type = (BackupOperationType) getIntent().getSerializableExtra("TYPE");
        if (type == null) type = BackupOperationType.UNDEFINED;

        switch (type) {
            case UNDEFINED:
                state = BackupState.UNDEFINED;
                DisplayKeyFailureDialog("Unknown Key operation");
                break;
            case BACKUP_HD_KEY:
                state = BackupState.ENTER_BACKUP_STATE_HD;
                WriteDownSeedPhrase();
                DisplaySeed();
                break;
            case BACKUP_KEYSTORE_KEY:
                state = BackupState.ENTER_JSON_BACKUP;
                JSONBackup();
                break;
            case SHOW_SEED_PHRASE:
                state = BackupState.SHOW_SEED_PHRASE;
                setupTestSeed();
                DisplaySeed();
                break;
            case EXPORT_PRIVATE_KEY:
                DisplayKeyFailureDialog("Export Private key not yet implemented");
                //TODO: Not yet implemented
                break;
            case UPGRADE_KEY:
                setupUpgradeKey(false);
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

    private void setupUpgradeKey(boolean showSuccess) {
        setContentView(R.layout.activity_backup);
        initViews();

        successOverlay = findViewById(R.id.layout_success_overlay);
        if (successOverlay != null && showSuccess) {
            successOverlay.setVisibility(View.VISIBLE);
            handler.postDelayed(this, 1000);
        }

        state = BackupState.UPGRADE_KEY_SECURITY;
        if (wallet.type == WalletType.KEYSTORE) {
            title.setText(R.string.lock_keystore_upgrade);
        } else {
            title.setText(R.string.lock_key_upgrade);
        }
        backupImage.setImageResource(R.drawable.biometrics);
        detail.setVisibility(View.VISIBLE);
        detail.setText(R.string.upgrade_key_security_detail);

        int res;
        if (wallet.type == WalletType.HDKEY) {
            res = R.string.lock_seed_phrase;
        } else {
            res = R.string.action_upgrade_key;
        }

        functionButtonBar.setPrimaryButtonText(res);
        functionButtonBar.setPrimaryButtonClickListener(this);
    }

    @Override
    public void keyUpgraded(final KeyService.UpgradeKeyResult result)
    {
        handler.post(() -> {
            switch (result)
            {
                case REQUESTING_SECURITY: //Deprecated
                    //Do nothing, callback will return to 'CreatedKey()'. If it fails the returned key is empty. //Update - this should never happen - remove
                    break;
                case NO_SCREENLOCK:
                    hasNoLock = true;
                    DisplayKeyFailureDialog("Unable to upgrade key: Enable screenlock on phone");
                    break;
                case ALREADY_LOCKED:
                    finishBackupSuccess(false); // already upgraded to top level
                    break;
                case ERROR:
                    hasNoLock = true;
                    DisplayKeyFailureDialog("Unable to upgrade key: Unknown Error");
                    break;
                case SUCCESSFULLY_UPGRADED:
                    createdKey(wallet.address);
                    break;
            }
        });
    }

    private void upgradeKeySecurity() {
        switch (wallet.type) {
            case KEYSTORE:
            case KEYSTORE_LEGACY:
            case HDKEY:
                viewModel.upgradeKeySecurity(wallet, this, this);
                break;

            default:
                break;
        }
    }

    @Override
    public void createdKey(String address) {
        //key upgraded
        //store wallet upgrade
        if (wallet.address.equalsIgnoreCase(address)) {
            switch (wallet.type) {
                case KEYSTORE_LEGACY:
                case KEYSTORE:
                case HDKEY:
                    viewModel.upgradeWallet(address);
                    finishBackupSuccess(true);
                    break;
                default:
                    cancelAuthentication();
                    break;
            }
        }
    }

    private void setupTestSeed() {
        setContentView(R.layout.activity_backup_write_seed);
        initViews();
    }

    @Override
    public void run()
    {
        if (successOverlay == null) return;
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

    @Override
    public void onPause() {
        super.onPause();

        viewModel.resetSignDialog();
        //hide seed phrase and any visible words
        if (layoutWordHolder != null) layoutWordHolder.removeAllViews();

        switch (state) {
            case WRITE_DOWN_SEED_PHRASE:
            case SHOW_SEED_PHRASE:
                //note, the OS calls onPause if user chooses to authenticate using PIN or password (takes them to the auth screen).
                WriteDownSeedPhrase();
                break;

            case SHOW_SEED_PHRASE_SINGLE:
                finish();
                break;

            case SEED_PHRASE_INVALID:
            case VERIFY_SEED_PHRASE:
                state = BackupState.ENTER_BACKUP_STATE_HD; //reset view back to splash screen
                WriteDownSeedPhrase();
                DisplaySeed();
                break;

            case SET_JSON_PASSWORD:
                JSONBackup();
                break;

            case ENTER_JSON_BACKUP:
            case ENTER_BACKUP_STATE_HD:
            case UPGRADE_KEY_SECURITY:
            case FINISH:
                break;
        }
    }

    private void initViews() {
        title = findViewById(R.id.text_title);
        detail = findViewById(R.id.text_detail);
        layoutWordHolder = findViewById(R.id.layout_word_holder);
        verifyTextBox = findViewById(R.id.text_verify);
        backupImage = findViewById(R.id.backup_seed_image);
        functionButtonBar = findViewById(R.id.layoutButtons);
        inputView = findViewById(R.id.input_password);
        if (inputView != null) {
            inputView.getEditText().addTextChangedListener(this);
        }
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        toolbar();
        setTitle(getString(R.string.empty));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        switch (state) {
            case VERIFY_SEED_PHRASE:
            case SEED_PHRASE_INVALID:
                //if we're currently verifying seed or we made a mistake copying the seed down then allow user to restart
                state = BackupState.WRITE_DOWN_SEED_PHRASE;
                WriteDownSeedPhrase();
                DisplaySeed();
                break;
            case WRITE_DOWN_SEED_PHRASE:
                state = BackupState.ENTER_BACKUP_STATE_HD;
                keyFailure("");
                break;
            case SET_JSON_PASSWORD:
                state = BackupState.ENTER_JSON_BACKUP;
                keyFailure("");
                break;
            case FINISH:
            case SHOW_SEED_PHRASE_SINGLE:
                state = BackupState.FINISH;
                finish();
                break;
            default:
                keyFailure("");
                break;
        }
    }

    @Override
    public void onClick(View view) {
        // Passing an empty String as this class handles clicks based on state
        handleClick("", 0);
    }

    private void ResetInputBox() {
        verifyTextBox.setBackgroundResource(R.drawable.background_verify_window);
        verifyTextBox.setTextColor(getColor(R.color.dove));
        verifyTextBox.setText(R.string.empty);
        TextView invalid = findViewById(R.id.text_invalid);
        invalid.setVisibility(View.GONE);
    }

    private void JSONBackup() {
        setContentView(R.layout.activity_set_json_password);
        initViews();
        setTitle(getString(R.string.set_keystore_password));
        inputView.setInstruction(R.string.password_6_characters_or_more);
        state = BackupState.SET_JSON_PASSWORD;
        inputView.getEditText().addTextChangedListener(this);
        functionButtonBar.setPrimaryButtonText(R.string.save_keystore);
        functionButtonBar.setPrimaryButtonClickListener(this);
        functionButtonBar.setPrimaryButtonEnabled(false);
        inputView.setLayoutListener(this, this);
    }

    private void TestSeedPhrase() {
        String currentText = verifyTextBox.getText().toString();
        String[] currentTest = currentText.split(" ");

        if (currentTest.length != mnemonicArray.length) {
            //fail. This should never happen
            seedIncorrect();
            return;
        } else {
            for (int i = 0; i < mnemonicArray.length; i++) {
                if (!mnemonicArray[i].equals(currentTest[i])) {
                    seedIncorrect();
                    return;
                }
            }
        }

        layoutWordHolder.setVisibility(View.GONE);
        verifyTextBox.setVisibility(View.GONE);

        //terminate and display tick
        backupKeySuccess(BackupOperationType.BACKUP_HD_KEY);
    }

    private void seedIncorrect() {
        verifyTextBox.setBackgroundResource(R.drawable.background_verify_window_fail);
        verifyTextBox.setTextColor(getColor(R.color.dove));
        TextView invalid = findViewById(R.id.text_invalid);
        invalid.setVisibility(View.VISIBLE);
        Toast.makeText(this, R.string.invalid_phrase, Toast.LENGTH_LONG).show();
        ResetInputBox();
        VerifySeedPhrase();
    }

    private void backupKeySuccess(BackupOperationType type) {
        //first record backup time success, in case user aborts operation during key locking
        viewModel.backupSuccess(wallet);

        //now ask if user wants to upgrade the key security (if required)
        switch (wallet.authLevel) {
            case STRONGBOX_NO_AUTHENTICATION:
            case TEE_NO_AUTHENTICATION:
                //improve key security
                setupUpgradeKey(true);
                break;
            default:
                finishBackupSuccess(true);
                break;
        }
    }

    private void finishBackupSuccess(boolean upgradeKey) {
        state = BackupState.SEED_PHRASE_VALIDATED;

        Intent intent = new Intent();
        switch (wallet.type) {
            case KEYSTORE_LEGACY:
            case KEYSTORE:
                intent.putExtra("TYPE", BackupOperationType.BACKUP_KEYSTORE_KEY);
                break;
            case HDKEY:
                intent.putExtra("TYPE", BackupOperationType.BACKUP_HD_KEY);
                break;
            default:
                cancelAuthentication();
                break;
        }

        intent.putExtra("Key", wallet.address);
        intent.putExtra("Upgrade", upgradeKey);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void VerifySeedPhrase() {
        setContentView(R.layout.activity_verify_seed_phrase);
        initViews();
        functionButtonBar.setPrimaryButtonText(R.string.action_continue);
        functionButtonBar.setPrimaryButtonClickListener(v -> TestSeedPhrase());
        functionButtonBar.setPrimaryButtonEnabled(false);
        state = BackupState.VERIFY_SEED_PHRASE;
        title.setText(R.string.verify_seed_phrase);
        TextView invalid = findViewById(R.id.text_invalid);
        invalid.setVisibility(View.INVISIBLE);
        layoutWordHolder.setVisibility(View.VISIBLE);
        layoutWordHolder.removeAllViews();

        if (mnemonicArray != null) {
            jumbleList();
        }
    }

    private void jumbleList() {
        List<Integer> numberList = new ArrayList<>();
        for (int i = 0; i < mnemonicArray.length; i++)
            numberList.add(i);

        for (int i = 0; i < mnemonicArray.length; i++) {
            int random = (int) (Math.random() * (double) numberList.size());
            int mnemonicIndex = numberList.get(random);
            numberList.remove(random); //remove this index
            TextView tv = generateSeedWordTextView(mnemonicArray[mnemonicIndex]);
            tv.setOnClickListener(view -> onWordClick(tv));
            layoutWordHolder.addView(tv);
        }
    }

    private void onWordClick(TextView tv) {
        tv.setTextColor(getColor(R.color.alabaster));
        tv.setBackgroundResource(R.drawable.background_seed_word_selected);
        tv.setOnClickListener(null);
        String currentText = verifyTextBox.getText().toString();
        if (currentText.length() > 0) currentText += " ";
        currentText += tv.getText().toString();
        verifyTextBox.setText(currentText);

        String[] currentTest = currentText.split(" ");
        if (currentTest.length == mnemonicArray.length)
        {
            functionButtonBar.setPrimaryButtonEnabled(true);
        }
    }

    private void WriteDownSeedPhrase() {
        setContentView(R.layout.activity_backup_write_seed);
        initViews();
        state = BackupState.WRITE_DOWN_SEED_PHRASE;
        title.setText(R.string.write_down_seed_phrase);
        functionButtonBar.setPrimaryButtonText(R.string.wrote_down_seed_phrase);
        functionButtonBar.setPrimaryButtonClickListener(this);
    }

    private void DisplaySeed() {
        if (layoutWordHolder != null)
        {
            layoutWordHolder.setVisibility(View.VISIBLE);
            layoutWordHolder.removeAllViews();
        }

        viewModel.getAuthentication(wallet, this, this);
    }

    private TextView generateSeedWordTextView(String word) {
        int margin = Utils.dp2px(this, 4);
        int padding;
        float textSize;
        int textViewHeight;

        if (screenWidth > 800)
        {
            textSize = 16.0f;
            padding = Utils.dp2px(this, 20);
            textViewHeight = Utils.dp2px(this, 44);
        }
        else
        {
            textSize = 14.0f;
            padding = Utils.dp2px(this, 16);
            textViewHeight = Utils.dp2px(this, 38);
        }

        FlexboxLayout.LayoutParams params =
                new FlexboxLayout.LayoutParams(FlexboxLayout.LayoutParams.WRAP_CONTENT, textViewHeight);

        params.setMargins(margin, margin, margin, margin);
        TextView seedWord = new TextView(this);
        seedWord.setMaxLines(1);
        seedWord.setText(word);
        seedWord.setTypeface(ResourcesCompat.getFont(this, R.font.font_regular));
        seedWord.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        seedWord.setBackgroundResource(R.drawable.background_seed_word);
        seedWord.setTextColor(getColor(R.color.mine));
        seedWord.setLayoutParams(params);
        seedWord.setGravity(Gravity.CENTER);
        seedWord.setPadding(padding, 0, padding, 0);

        return seedWord;
    }

    @Override
    public void HDKeyCreated(String address, Context ctx, KeyService.AuthenticationLevel level) {
        //empty, doesn't get called
    }

    @Override
    public void keyFailure(String message) {
        if (message != null && message.length() > 0)
        {
            DisplayKeyFailureDialog(message);
        }
        else
        {
            Intent intent = new Intent();
            setResult(RESULT_CANCELED, intent);
            intent.putExtra("Key", wallet.address);
            finish();
        }
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
            cancelAuthentication();
            alertDialog.dismiss();
        });
        alertDialog.setOnCancelListener(v -> {
            cancelAuthentication();
        });
        alertDialog.show();
    }

    @Override
    public void fetchMnemonic(String mnemonic) {
        handler.post(() -> {
            switch (state) {
                case WRITE_DOWN_SEED_PHRASE:
                    WriteDownSeedPhrase();
                    mnemonicArray = mnemonic.split(" ");
                    addSeedWordsToScreen();
                    break;
                case ENTER_JSON_BACKUP:
                case SET_JSON_PASSWORD:
                    viewModel.exportWallet(wallet, mnemonic, keystorePassword);
                    break;
                case SHOW_SEED_PHRASE:
                    setupTestSeed(); //drop through
                case SHOW_SEED_PHRASE_SINGLE:
                    mnemonicArray = mnemonic.split(" ");
                    addSeedWordsToScreen();
                    break;
                case VERIFY_SEED_PHRASE:
                    VerifySeedPhrase();
                    mnemonicArray = mnemonic.split(" ");
                    addSeedWordsToScreen();
                    break;
                case SEED_PHRASE_INVALID:
                case UNDEFINED:
                case ENTER_BACKUP_STATE_HD:
                case UPGRADE_KEY_SECURITY:
                    DisplayKeyFailureDialog("Error in key restore: " + state.ordinal());
                    break;
            }
        });
    }

    private void addSeedWordsToScreen() {
        if (mnemonicArray == null) return;
        layoutWordHolder.setFlexDirection(FlexDirection.ROW);

        for (String word : mnemonicArray) {
            layoutWordHolder.addView(generateSeedWordTextView(word));
        }
    }

    @Override
    public void gotAuthorisation(boolean gotAuth)
    {
        if (gotAuth)
        {
            //use this to get seed backup
            switch (state)
            {
                case UNDEFINED:
                    break;
                case ENTER_BACKUP_STATE_HD:
                    break;
                case WRITE_DOWN_SEED_PHRASE:
                    //proceed and get the mnemonic
                    viewModel.getSeedPhrase(wallet, this, this);
                    break;
                case VERIFY_SEED_PHRASE:
                    break;
                case SEED_PHRASE_INVALID:
                    break;
                case ENTER_JSON_BACKUP:
                case SET_JSON_PASSWORD:
                    viewModel.getPasswordForKeystore(wallet, this, this);
                    break;
                case SHOW_SEED_PHRASE_SINGLE:
                case SHOW_SEED_PHRASE:
                    viewModel.getSeedPhrase(wallet, this, this);
                    break;
                case UPGRADE_KEY_SECURITY:
                    upgradeKeySecurity();
                    break;
            }
        }
        else
        {
            DisplayKeyFailureDialog(getString(R.string.authentication_error));
        }
    }

    @Override
    public void cancelAuthentication()
    {
        Intent intent = new Intent();
        setResult(RESULT_CANCELED, intent);
        intent.putExtra("Key", wallet.address);
        if (hasNoLock) intent.putExtra("nolock", true);
        finish();
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this)
                .get(BackupKeyViewModel.class);
        viewModel.exportedStore().observe(this, this::onExportKeystore);
    }

    ActivityResultLauncher<Intent> handleBackupWallet = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    backupKeySuccess(BackupOperationType.BACKUP_KEYSTORE_KEY);
                } else {
                    AskUserSuccess();
                }
            });

    private void onExportKeystore(String keystore) {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Keystore");
        sharingIntent.putExtra(Intent.EXTRA_TEXT, keystore);
        handleBackupWallet.launch(Intent.createChooser(sharingIntent, "Share via"));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Operation taskCode = null;

        //Interpret the return code; if it's within the range of values possible to return from PIN confirmation then separate out
        //the task code from the return value. We have to do it this way because there's no way to send a bundle across the PIN dialog
        //and out through the PIN dialog's return back to here
        if (requestCode >= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS && requestCode <= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS + 10) {
            taskCode = Operation.values()[requestCode - SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS];
            requestCode = SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS;
        }

        switch (requestCode) {
            case SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS:
                if (resultCode == RESULT_OK) {
                    viewModel.completeAuthentication(taskCode);
                } else {
                    viewModel.failedAuthentication(taskCode);
                }
                break;
        }
    }

    private void AskUserSuccess() {
        hideDialog();
        alertDialog = new AWalletAlertDialog(this);
        alertDialog.setIcon(AWalletAlertDialog.SUCCESS);
        alertDialog.setTitle(R.string.do_manage_make_backup);
        alertDialog.setButtonText(R.string.yes_continue);
        alertDialog.setButtonListener(v -> {
            hideDialog();
            backupKeySuccess(BackupOperationType.BACKUP_KEYSTORE_KEY);
        });
        alertDialog.setSecondaryButtonText(R.string.no_repeat);
        alertDialog.setSecondaryButtonListener(v -> {
            hideDialog();
            cancelAuthentication();
        });
        alertDialog.show();
    }

    private void hideDialog() {
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
            alertDialog = null;
        }
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void afterTextChanged(Editable editable) {
        switch (state) {
            case ENTER_BACKUP_STATE_HD:
                break;
            case WRITE_DOWN_SEED_PHRASE:
                break;
            case VERIFY_SEED_PHRASE:
                break;
            case SEED_PHRASE_INVALID:
                break;
            case ENTER_JSON_BACKUP:
                break;
            case SET_JSON_PASSWORD:
                String txt = inputView.getText().toString();
                //password length minimum 6
                functionButtonBar.setPrimaryButtonEnabled(txt.length() >= 6);
                break;
            case SHOW_SEED_PHRASE:
                break;
        }
    }

    @Override
    public void onLayoutShrunk() {

    }

    @Override
    public void onLayoutExpand() {

    }

    @Override
    public void onInputDoneClick(View view) {
        inputView = findViewById(R.id.input_password);
        keystorePassword = inputView.getText().toString();
        if (keystorePassword.length() > 5)
        {
            //get authentication
            viewModel.getAuthentication(wallet, this, this);
        }
        else
        {
            inputView.setError(R.string.password_6_characters_or_more);
        }
    }

    @Override
    public void handleClick(String action, int id) {
        switch (state) {
            case ENTER_BACKUP_STATE_HD:
                WriteDownSeedPhrase();
                DisplaySeed();
                break;
            case WRITE_DOWN_SEED_PHRASE:
            case SHOW_SEED_PHRASE:
                VerifySeedPhrase();
                break;
            case VERIFY_SEED_PHRASE:
                TestSeedPhrase();
                break;
            case SEED_PHRASE_INVALID:
                ResetInputBox();
                VerifySeedPhrase();
                break;
            case SHOW_SEED_PHRASE_SINGLE:
                state = BackupState.FINISH;
                finish();
                break;
            case ENTER_JSON_BACKUP:
                JSONBackup();
                break;
            case SET_JSON_PASSWORD:
                inputView = findViewById(R.id.input_password);
                onInputDoneClick(inputView);
                break;
            case UPGRADE_KEY_SECURITY:
                //first open authentication
                viewModel.getAuthentication(wallet, this, this);
                break;
        }
    }

    private void secureWindow()
    {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
    }
}
