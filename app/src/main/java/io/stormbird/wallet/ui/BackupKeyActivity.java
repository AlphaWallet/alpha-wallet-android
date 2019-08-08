package io.stormbird.wallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.*;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import dagger.android.AndroidInjection;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.*;
import io.stormbird.wallet.service.KeyService;
import io.stormbird.wallet.viewmodel.BackupKeyViewModel;
import io.stormbird.wallet.viewmodel.BackupKeyViewModelFactory;
import io.stormbird.wallet.widget.AWalletAlertDialog;
import io.stormbird.wallet.widget.PasswordInputView;
import io.stormbird.wallet.widget.SignTransactionDialog;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static io.stormbird.wallet.C.Key.WALLET;
import static io.stormbird.wallet.C.SHARE_REQUEST_CODE;

public class BackupKeyActivity extends BaseActivity implements View.OnClickListener,
        CreateWalletCallbackInterface, TextWatcher, SignAuthenticationCallback
{
    @Inject
    BackupKeyViewModelFactory backupKeyViewModelFactory;
    BackupKeyViewModel viewModel;

    private BackupState state;
    private String keyBackup;
    private Wallet wallet;
    private TextView title;
    private TextView detail;
    private TextView passwordDetail;
    private TextView passwordLengthNote;
    private LinearLayout layoutHolder;
    private LinearLayout layoutWordHolder;
    private PasswordInputView inputView;
    private ImageView backupImage;
    private Button nextButton;
    private TextView verifyTextBox;
    private LinearLayout skipButton;
    private String[] mnemonicArray;
    private PinAuthenticationCallbackInterface authInterface;

    private AWalletAlertDialog alertDialog;

    private LinearLayout currentHolder;
    private int currentEdge;
    private int layoutHolderWidth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        alertDialog = null;

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_backup);

        toolbar();

        String type = getIntent().getStringExtra("TYPE");
        keyBackup = getIntent().getStringExtra("ADDRESS");
        wallet = getIntent().getParcelableExtra(WALLET);
        layoutHolderWidth = 0;
        initViews();

        switch (type)
        {
            case "HDKEY":
                state = BackupState.ENTER_BACKUP_STATE_HD;
                setHDBackupSplash();
                break;
            case "JSON":
                state = BackupState.ENTER_JSON_BACKUP;
                setupJSONExport();
                break;
            case "TEST_SEED":
                state = BackupState.TEST_SEED_PHRASE;
                setTitle(getString(R.string.seed_phrase));
                break;
        }

        initViewModel();
    }

    private void resetView()
    {
        inputView.setVisibility(View.GONE);
        passwordDetail.setVisibility(View.GONE);
        passwordLengthNote.setVisibility(View.GONE);
        title.setVisibility(View.VISIBLE);
        backupImage.setVisibility(View.VISIBLE);
        detail.setVisibility(View.VISIBLE);
        skipButton.setVisibility(View.GONE);
        layoutWordHolder.setVisibility(View.GONE);
        setBottomButtonActive(true);
    }

    private void setupUpgradeKey()
    {
        resetView();

        setTitle(getString(R.string.action_upgrade_key));
        state = BackupState.UPGRADE_KEY_SECURITY;
        if (wallet.type == WalletType.KEYSTORE) title.setText(R.string.lock_keystore_upgrade);
        else title.setText(R.string.lock_key_upgrade);
        backupImage.setImageResource(R.drawable.ic_biometric);
        detail.setVisibility(View.VISIBLE);
        detail.setText(R.string.upgrade_key_security_detail);
        nextButton.setText(getString(R.string.action_upgrade_key));
        skipButton.setVisibility(View.VISIBLE);

        skipButton.setOnClickListener(v -> {
            finishBackupSuccess(false);
        });
    }

    private void upgradeKeySecurity()
    {
        switch (wallet.type)
        {
            case KEYSTORE:
            case KEYSTORE_LEGACY:
            case HDKEY:
                switch (viewModel.upgradeKeySecurity(keyBackup, this, this))
                {
                    case REQUESTING_SECURITY:
                        //Do nothing, callback will return to 'CreatedKey()'. If it fails the returned key is empty
                        break;
                    case NO_SCREENLOCK:
                        DisplayKeyFailureDialog("Unable to upgrade key: Enable screenlock on phone");
                        break;
                    case ALREADY_LOCKED:
                        finishBackupSuccess(false); // already upgraded to top level
                        break;
                    case ERROR:
                        DisplayKeyFailureDialog("Unable to upgrade key: Unknown Error");
                        break;
                }

            default:
                break;
        }
    }

    @Override
    public void CreatedKey(String address)
    {
        //key upgraded
        //store wallet upgrade
        Wallet wallet = new Wallet(address);
        wallet.checkWalletType(this);
        switch (wallet.type)
        {
            case KEYSTORE_LEGACY:
            case KEYSTORE:
            case HDKEY:
                viewModel.upgradeWallet(keyBackup);
                finishBackupSuccess(true);
                break;
            default:
                cancelAuthentication();
                break;
        }
    }

    private void setHDBackupSplash()
    {
        setTitle(getString(R.string.title_backup_seed));
        title.setText(R.string.backup_seed_phrase);
        backupImage.setImageResource(R.drawable.seed_graphic);
        detail.setVisibility(View.VISIBLE);
        detail.setText(R.string.backup_seed_phrase_detail);
        nextButton.setText(R.string.action_back_up_my_wallet);
        state = BackupState.ENTER_BACKUP_STATE_HD;
    }

    private void setupJSONExport()
    {
        setTitle(getString(R.string.export_keystore_json));
        title.setText(R.string.export_keystore_json);
        backupImage.setImageResource(R.drawable.ic_keystore);
        detail.setVisibility(View.VISIBLE);
        detail.setText(R.string.keystore_detail_text);
        nextButton.setText(R.string.export_keystore_json);
        state = BackupState.ENTER_JSON_BACKUP;
    }

    private void setupTestSeed()
    {
        title.setText(getString(R.string.make_a_backup, "12"));
        backupImage.setVisibility(View.GONE);
        detail.setVisibility(View.GONE);
        nextButton.setText(R.string.test_seed_phrase);
        DisplaySeed();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        //hide seed phrase and any visible words
        layoutWordHolder.removeAllViews();
        verifyTextBox.setText("");

        switch (state)
        {
            case SEED_PHRASE_INVALID:
            case WRITE_DOWN_SEED_PHRASE:
            case VERIFY_SEED_PHRASE:
            case TEST_SEED_PHRASE:
                resetView();
                state = BackupState.ENTER_BACKUP_STATE_HD;
                break;

            case SET_JSON_PASSWORD:
                resetView();
                state = BackupState.ENTER_JSON_BACKUP;
                break;

            case ENTER_JSON_BACKUP:
            case ENTER_BACKUP_STATE_HD:
            case UPGRADE_KEY_SECURITY:
                break;
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        initViews();

        switch (state)
        {
            case ENTER_BACKUP_STATE_HD:
                break;

            case ENTER_JSON_BACKUP:
                setupJSONExport();
                break;

            default:
                break;
        }
    }

    private void initViews()
    {
        title = findViewById(R.id.text_title);
        detail = findViewById(R.id.text_detail);
        passwordDetail = findViewById(R.id.text_detail_password);
        passwordLengthNote = findViewById(R.id.text_password_length);
        layoutHolder = findViewById(R.id.layout_center_holder);
        layoutWordHolder = findViewById(R.id.layout_word_holder);
        nextButton = findViewById(R.id.button_next);
        verifyTextBox = findViewById(R.id.text_verify);
        backupImage = findViewById(R.id.seed_image);
        inputView = findViewById(R.id.input_password);
        nextButton.setOnClickListener(this);
        skipButton = findViewById(R.id.button_cancel);
        inputView.getEditText().addTextChangedListener(this);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        // Note: The seed display requires the holder view to be drawn so it can measure how much
        // space is left on each line.
        ViewTreeObserver vto = layoutHolder.getViewTreeObserver();
        vto.addOnGlobalLayoutListener (() -> {
            if (layoutHolderWidth == 0)
            {
                layoutHolderWidth = layoutHolder.getMeasuredWidth();
                if (state == BackupState.TEST_SEED_PHRASE && layoutHolderWidth != 0)
                {
                    setupTestSeed();
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                switch (state)
                {
                    case VERIFY_SEED_PHRASE:
                    case SEED_PHRASE_INVALID:
                        //if we're currently verifying seed or we made a mistake copying the seed down then allow user to restart
                        state = BackupState.WRITE_DOWN_SEED_PHRASE;
                        WriteDownSeedPhrase();
                        break;
                    case WRITE_DOWN_SEED_PHRASE:
                        resetView();
                        setHDBackupSplash();
                        break;
                    case SET_JSON_PASSWORD:
                        resetView();
                        setupJSONExport();
                        break;
                    default:
                        keyFailure("");
                        break;
                }
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view)
    {
        switch (state)
        {
            case ENTER_BACKUP_STATE_HD:
                WriteDownSeedPhrase();
                break;
            case WRITE_DOWN_SEED_PHRASE:
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
            case SET_JSON_PASSWORD:
                viewModel.getPasswordForKeystore(keyBackup, this, this);
                break;
            case TEST_SEED_PHRASE:
                VerifySeedPhrase();
                break;
            case UPGRADE_KEY_SECURITY:
                upgradeKeySecurity();
                break;
        }
    }

    private void ResetInputBox()
    {
        verifyTextBox.setBackgroundResource(R.drawable.background_verify_window);
        verifyTextBox.setTextColor(getColor(R.color.greyaa));
        verifyTextBox.setText(R.string.empty);
        TextView invalid = findViewById(R.id.text_invalid);
        invalid.setVisibility(View.GONE);

        nextButton.setText(R.string.action_continue);
    }

    private void JSONBackup()
    {
        setTitle(getString(R.string.set_keystore_password));
        inputView.setVisibility(View.VISIBLE);
        passwordDetail.setVisibility(View.VISIBLE);
        state = BackupState.SET_JSON_PASSWORD;
        title.setVisibility(View.GONE);
        backupImage.setVisibility(View.GONE);
        detail.setVisibility(View.GONE);
        passwordDetail.setText(R.string.keystore_loss_warning);
        passwordLengthNote.setVisibility(View.VISIBLE);
        inputView.getEditText().addTextChangedListener(this);
        nextButton.setText(R.string.share_keystore);
        updateButtonState(false);
    }

    private void TestSeedPhrase()
    {
        String currentText = verifyTextBox.getText().toString();
        String[] currentTest = currentText.split(" ");

        if (currentTest.length != mnemonicArray.length)
        {
            //fail. This should never happen
            seedIncorrect();
            return;
        }
        else
        {
            for (int i = 0; i < mnemonicArray.length; i++)
            {
                if (!mnemonicArray[i].equals(currentTest[i])) { seedIncorrect(); return; }
            }
        }

        layoutWordHolder.setVisibility(View.GONE);
        verifyTextBox.setVisibility(View.GONE);

        //terminate and display tick
        backupKeySuccess("HDKEY");
    }

    private void seedIncorrect()
    {
        verifyTextBox.setBackgroundResource(R.drawable.background_verify_window_fail);
        verifyTextBox.setTextColor(getColor(R.color.grey_faint));
        TextView invalid = findViewById(R.id.text_invalid);
        invalid.setVisibility(View.VISIBLE);

        nextButton.setText(R.string.try_again);
        state = BackupState.SEED_PHRASE_INVALID;
    }

    private void backupKeySuccess(String keyType)
    {
        //first record backup time success, in case user aborts operation during key locking
        viewModel.backupSuccess(keyBackup);

        //now ask if user wants to upgrade the key security (if required)
        wallet = new Wallet(keyBackup);
        wallet.checkWalletType(this);

        switch (wallet.authLevel)
        {
            case STRONGBOX_NO_AUTHENTICATION:
            case TEE_NO_AUTHENTICATION:
                //improve key security
                setupUpgradeKey();
                break;
            default:
                finishBackupSuccess(true);
                break;
        }
    }

    private void finishBackupSuccess(boolean upgradeKey)
    {
        Wallet wallet = new Wallet(keyBackup);
        wallet.checkWalletType(this);
        Intent intent = new Intent();
        switch (wallet.type)
        {
            case KEYSTORE_LEGACY:
            case KEYSTORE:
                intent.putExtra("TYPE", "JSON");
                break;
            case HDKEY:
                intent.putExtra("TYPE", "HDKEY");
                break;
            default:
                cancelAuthentication();
                break;
        }

        intent.putExtra("Key", keyBackup);
        intent.putExtra("Upgrade", upgradeKey);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void VerifySeedPhrase()
    {
        state = BackupState.VERIFY_SEED_PHRASE;
        title.setText(R.string.verify_seed_phrase);
        nextButton.setText(R.string.action_continue);
        verifyTextBox.setVisibility(View.VISIBLE);
        TextView invalid = findViewById(R.id.text_invalid);
        invalid.setVisibility(View.INVISIBLE);
        layoutWordHolder.setVisibility(View.VISIBLE);
        layoutWordHolder.removeAllViews();
        setBottomButtonActive(false);

        if (mnemonicArray == null) return;

        addNewLayoutLine();

        //jumble list
        List<Integer> numberList = new ArrayList<>();
        for (int i = 0; i < mnemonicArray.length; i++) numberList.add(i);

        for (int i = 0; i < mnemonicArray.length; i++)
        {
            int random = (int)(Math.random()*(double)numberList.size());
            int mnemonicIndex = numberList.get(random);
            numberList.remove(random); //remove this index
            TextView tv = addWordToLayout(mnemonicArray[mnemonicIndex]);
            tv.setOnClickListener(view -> onWordClick(tv));
        }
    }

    private void onWordClick(TextView tv)
    {
        tv.setTextColor(getColor(R.color.greyf9));
        tv.setBackgroundResource(R.drawable.background_seed_word_selected);
        tv.setOnClickListener(view -> { }); //clicking does nothing now
        String currentText = verifyTextBox.getText().toString();
        if (currentText.length() > 0) currentText += " ";
        currentText += tv.getText().toString();
        verifyTextBox.setText(currentText);

        String[] currentTest = currentText.split(" ");
        if (currentTest.length == mnemonicArray.length)
        {
            TestSeedPhrase();
            setBottomButtonActive(true);
        }
    }

    private void setBottomButtonActive(boolean active)
    {
        nextButton.setClickable(active);
        nextButton.setActivated(active);
        nextButton.setBackgroundColor(getColor(active ? R.color.nasty_green : R.color.inactive_green));
    }

    private void WriteDownSeedPhrase()
    {
        ResetInputBox();
        verifyTextBox.setVisibility(View.GONE);

        state = BackupState.WRITE_DOWN_SEED_PHRASE;
        backupImage.setVisibility(View.GONE);
        title.setText(R.string.write_down_seed_phrase);
        detail.setVisibility(View.GONE);
        nextButton.setText(R.string.wrote_down_seed_phrase);
        setBottomButtonActive(true);

        DisplaySeed();
    }

    private void DisplaySeed()
    {
        layoutWordHolder.setVisibility(View.VISIBLE);
        layoutWordHolder.removeAllViews();

        viewModel.getSeedPhrase(keyBackup, this, this);
    }

    private void addNewLayoutLine()
    {
        currentEdge = 0;
        currentHolder = new LinearLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0,0,0,8);
        currentHolder.setLayoutParams(params);
        currentHolder.setOrientation(LinearLayout.HORIZONTAL);
        layoutWordHolder.addView(currentHolder);
    }

    private TextView addWordToLayout(String word)
    {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0,0,8,0);

        TextView seedWord = new TextView(this);
        seedWord.setText(word);
        seedWord.setGravity(Gravity.CENTER_HORIZONTAL);
        seedWord.setBackgroundResource(R.drawable.background_seed_word);
        seedWord.setTextColor(getColor(R.color.greyaa));
        seedWord.setLayoutParams(params);
        seedWord.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        seedWord.setPadding(40,25,40,25);

        seedWord.measure(0, 0);
        int measuredWidth = seedWord.getMeasuredWidth();
        if ((currentEdge + measuredWidth + 8) > layoutHolderWidth)
        {
            addNewLayoutLine();
        }

        currentEdge += (measuredWidth + 8);

        currentHolder.addView(seedWord);
        return seedWord;
    }

    @Override
    public void HDKeyCreated(String address, Context ctx, KeyService.AuthenticationLevel level)
    {
        //empty, doesn't get called
    }

    @Override
    public void keyFailure(String message)
    {
        if (message != null && message.length() > 0)
        {
            DisplayKeyFailureDialog(message);
        }
        else
        {
            Intent intent = new Intent();
            setResult(RESULT_CANCELED, intent);
            finish();
        }
    }

    private void DisplayKeyFailureDialog(String message)
    {
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
    public void cancelAuthentication()
    {
        Intent intent = new Intent();
        setResult(RESULT_CANCELED, intent);
        finish();
    }

    @Override
    public void FetchMnemonic(String mnemonic)
    {
        if (state == BackupState.SET_JSON_PASSWORD || state == BackupState.ENTER_JSON_BACKUP)
        {
            viewModel.exportWallet(keyBackup, mnemonic, inputView.getText().toString());
        }
        else
        {
            addNewLayoutLine();
            mnemonicArray = mnemonic.split(" ");
            //start adding words to layout
            for (String word : mnemonicArray)
            {
                addWordToLayout(word);
            }
        }
    }

    @Override
    public void GotAuthorisation(boolean gotAuth)
    {

    }

    @Override
    public void setupAuthenticationCallback(PinAuthenticationCallbackInterface authCallback)
    {
        authInterface = authCallback;
    }

    private void initViewModel()
    {
        viewModel = ViewModelProviders.of(this, backupKeyViewModelFactory)
                .get(BackupKeyViewModel.class);
        viewModel.exportedStore().observe(this, this::onExportKeystore);
    }

    private void onExportKeystore(String keystore)
    {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Keystore");
        sharingIntent.putExtra(Intent.EXTRA_TEXT, keystore);
        startActivityForResult(
                Intent.createChooser(sharingIntent, "Share via"),
                SHARE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        int taskCode = 0;

        //Interpret the return code; if it's within the range of values possible to return from PIN confirmation then separate out
        //the task code from the return value. We have to do it this way because there's no way to send a bundle across the PIN dialog
        //and out through the PIN dialog's return back to here
        if (requestCode >= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS && requestCode <= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS + 10)
        {
            taskCode = requestCode - SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS;
            requestCode = SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS;
        }

        switch (requestCode)
        {
            case SHARE_REQUEST_CODE:
                if (resultCode == RESULT_OK)
                {
                    backupKeySuccess("JSON");
                }
                else
                {
                    AskUserSuccess();
                }
                break;

            case SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS:
                if (resultCode == RESULT_OK)
                {
                    authInterface.CompleteAuthentication(taskCode);
                }
                else
                {
                    authInterface.FailedAuthentication(taskCode);
                }
                break;
        }
    }

    private void AskUserSuccess()
    {
        hideDialog();
        alertDialog = new AWalletAlertDialog(this);
        alertDialog.setIcon(AWalletAlertDialog.SUCCESS);
        alertDialog.setTitle(R.string.do_manage_make_backup);
        alertDialog.setButtonText(R.string.yes_continue);
        alertDialog.setButtonListener(v -> {
            hideDialog();
            backupKeySuccess("JSON");
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

    private void updateButtonState(boolean enabled)
    {
        nextButton.setActivated(enabled);
        nextButton.setClickable(enabled);
        int colorId = enabled ? R.color.nasty_green : R.color.inactive_green;
        if (getApplicationContext() != null) nextButton.setBackgroundColor(getColor(colorId));
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
    {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
    {

    }

    @Override
    public void afterTextChanged(Editable editable)
    {
        switch (state)
        {
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
                if (txt.length() >= 6) //password length minimum 6
                {
                    updateButtonState(true);
                }
                else
                {
                    updateButtonState(false);
                }
                break;
            case TEST_SEED_PHRASE:
                break;
        }
    }

    private enum BackupState
    {
        ENTER_BACKUP_STATE_HD, WRITE_DOWN_SEED_PHRASE, VERIFY_SEED_PHRASE, SEED_PHRASE_INVALID,
        ENTER_JSON_BACKUP, SET_JSON_PASSWORD, TEST_SEED_PHRASE, UPGRADE_KEY_SECURITY
    }
}
