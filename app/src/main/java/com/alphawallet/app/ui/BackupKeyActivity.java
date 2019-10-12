package com.alphawallet.app.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import com.alphawallet.app.R;
import com.alphawallet.app.entity.CreateWalletCallbackInterface;
import com.alphawallet.app.entity.Operation;
import com.alphawallet.app.entity.PinAuthenticationCallbackInterface;
import com.alphawallet.app.entity.SignAuthenticationCallback;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.viewmodel.BackupKeyViewModel;
import com.alphawallet.app.viewmodel.BackupKeyViewModelFactory;
import com.alphawallet.app.widget.AWalletAlertDialog;
import com.alphawallet.app.widget.PasswordInputView;
import com.alphawallet.app.widget.SignTransactionDialog;

import static com.alphawallet.app.C.Key.WALLET;
import static com.alphawallet.app.C.SHARE_REQUEST_CODE;

public class BackupKeyActivity extends BaseActivity implements View.OnClickListener,
        CreateWalletCallbackInterface, TextWatcher, SignAuthenticationCallback, Runnable
{
    @Inject
    BackupKeyViewModelFactory backupKeyViewModelFactory;
    BackupKeyViewModel viewModel;

    private BackupState state;
    private Wallet wallet;
    private TextView title;
    private TextView detail;
    private LinearLayout layoutWordHolder;
    private PasswordInputView inputView;
    private ImageView backupImage;
    private Button nextButton;
    private TextView verifyTextBox;
    private LinearLayout skipButton;
    private String[] mnemonicArray;
    private PinAuthenticationCallbackInterface authInterface;
    private ImageView spacerImage;
    private LinearLayout successOverlay;
    private Handler handler;

    private AWalletAlertDialog alertDialog;

    private LinearLayout currentHolder;
    private int currentEdge;

    private int layoutHolderWidth;
    private int seedTextSize;
    private int seedTextHPadding;
    private int seedTextVPadding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        alertDialog = null;

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        LockOrientation();

        BackupOperationType type = (BackupOperationType) getIntent().getSerializableExtra("TYPE");
        wallet = getIntent().getParcelableExtra(WALLET);
        layoutHolderWidth = 0;
        if (type == null) type = BackupOperationType.UNDEFINED;

        toolbar();
        initViewModel();

        switch (type)
        {
            case UNDEFINED:
                state = BackupState.UNDEFINED;
                DisplayKeyFailureDialog("Unknown Key operation");
                break;
            case BACKUP_HD_KEY:
                state = BackupState.ENTER_BACKUP_STATE_HD;
                setHDBackupSplash();
                break;
            case BACKUP_KEYSTORE_KEY:
                state = BackupState.ENTER_JSON_BACKUP;
                setupJSONExport();
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
        }
    }

    private void LockOrientation()
    {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    private void setupUpgradeKey()
    {
        setContentView(R.layout.activity_backup);
        initViews();

        successOverlay = findViewById(R.id.layout_success_overlay);
        if (successOverlay != null) successOverlay.setVisibility(View.VISIBLE);
        handler = new Handler();
        handler.postDelayed(this, 1000);

        setTitle(getString(R.string.empty));
        state = BackupState.UPGRADE_KEY_SECURITY;
        if (wallet.type == WalletType.KEYSTORE) title.setText(R.string.lock_keystore_upgrade);
        else title.setText(R.string.lock_key_upgrade);
        backupImage.setImageResource(R.drawable.ic_biometric);
        detail.setVisibility(View.VISIBLE);
        detail.setText(R.string.upgrade_key_security_detail);
        switch (wallet.type)
        {
            default:
                nextButton.setText(getString(R.string.action_upgrade_key));
                break;
            case HDKEY:
                nextButton.setText(getString(R.string.lock_seed_phrase));
                break;
        }

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
                switch (viewModel.upgradeKeySecurity(wallet, this, this))
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
        if (wallet.address.equalsIgnoreCase(address))
        {
            switch (wallet.type)
            {
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

    private void setHDBackupSplash()
    {
        setContentView(R.layout.activity_backup);
        initViews();

        setTitle(getString(R.string.empty));
        title.setText(R.string.backup_seed_phrase);
        backupImage.setImageResource(R.drawable.seed_graphic);
        detail.setText(R.string.backup_seed_phrase_detail);
        nextButton.setText(R.string.action_back_up_my_wallet);
    }

    private void setupJSONExport()
    {
        setContentView(R.layout.activity_json_backup);
        initViews();

        setTitle(getString(R.string.export_keystore_json));
        title.setText(R.string.what_is_keystore_json);
        backupImage.setImageResource(R.drawable.ic_keystore);
        detail.setText(R.string.keystore_detail_text);
        nextButton.setText(R.string.export_keystore_json);
        state = BackupState.ENTER_JSON_BACKUP;
    }

    private void setupTestSeed()
    {
        setContentView(R.layout.activity_show_seed_phrase);
        initViews();

        setTitle(getString(R.string.empty));
        title.setText(getString(R.string.make_a_backup, "12"));
        nextButton.setText(R.string.test_seed_phrase);
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
            handler = null;
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        viewModel.resetSignDialog();
        //hide seed phrase and any visible words
        if (layoutWordHolder != null) layoutWordHolder.removeAllViews();

        switch (state)
        {
            case WRITE_DOWN_SEED_PHRASE:
            case SHOW_SEED_PHRASE:
                setHDBackupSplash(); //note, the OS calls onPause if user chooses to authenticate using PIN or password (takes them to the auth screen).
                break;

            case SEED_PHRASE_INVALID:
            case VERIFY_SEED_PHRASE:
                state = BackupState.ENTER_BACKUP_STATE_HD; //reset view back to splash screen
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

    private void initViews()
    {
        title = findViewById(R.id.text_title);
        detail = findViewById(R.id.text_detail);
        layoutWordHolder = findViewById(R.id.layout_word_holder);
        nextButton = findViewById(R.id.button_next);
        verifyTextBox = findViewById(R.id.text_verify);
        backupImage = findViewById(R.id.seed_image);
        if (layoutHolderWidth > 0 && layoutHolderWidth <= 800)
        {
            setBackupImageSmall();
        }
        inputView = findViewById(R.id.input_password);
        nextButton.setOnClickListener(this);
        skipButton = findViewById(R.id.button_cancel);
        spacerImage = findViewById(R.id.spacer_image);
        if (inputView != null) inputView.getEditText().addTextChangedListener(this);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        // Note: The seed display requires the holder view to be drawn so it can measure how much
        // space is left on each line.
        if (layoutHolderWidth == 0 && layoutWordHolder != null)
        {
            ViewTreeObserver vto = layoutWordHolder.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(() -> {
                if (layoutHolderWidth == 0)
                {
                    //This is a little bit of a kludge, but it means the generated seed words
                    //adapt to the size of the screen. It's required for old phones with low resolution display.
                    //Tested against the oldest possible phone (Android 6) with a 480 pixel width.
                    layoutHolderWidth = layoutWordHolder.getMeasuredWidth();
                    if (layoutHolderWidth > 800)
                    {
                        seedTextVPadding = 25;
                        seedTextSize = 20;
                    }
                    else if (layoutHolderWidth > 600)
                    {
                        setBackupImageSmall();
                        seedTextVPadding = 15;
                        seedTextSize = 18;
                    }
                    else
                    {
                        setBackupImageSmall();
                        seedTextVPadding = 8;
                        seedTextSize = 16;
                    }
                    seedTextHPadding = layoutHolderWidth / 25;

                    // may need to resume
                    switch (state)
                    {
                        case SHOW_SEED_PHRASE:
                            addSeedWordsToScreen();
                        default:
                            break;
                    }
                }
            });
        }

        toolbar();
    }

    private void setBackupImageSmall()
    {
        if (backupImage != null) backupImage.setVisibility(View.GONE);
        backupImage = findViewById(R.id.seed_image_small);
        if (backupImage != null) backupImage.setVisibility(View.VISIBLE);
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
                        DisplaySeed();
                        break;
                    case WRITE_DOWN_SEED_PHRASE:
                        state = BackupState.ENTER_BACKUP_STATE_HD;
                        setHDBackupSplash();
                        break;
                    case SET_JSON_PASSWORD:
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
                DisplaySeed();
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
                viewModel.getPasswordForKeystore(wallet, this, this);
                break;
            case SHOW_SEED_PHRASE:
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
        setContentView(R.layout.activity_set_json_password);
        initViews();

        setTitle(getString(R.string.set_keystore_password));
        inputView.setInstruction(R.string.password_6_characters_or_more);
        state = BackupState.SET_JSON_PASSWORD;
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
        backupKeySuccess(BackupOperationType.BACKUP_HD_KEY);
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

    private void backupKeySuccess(BackupOperationType type)
    {
        //first record backup time success, in case user aborts operation during key locking
        viewModel.backupSuccess(wallet);

        //now ask if user wants to upgrade the key security (if required)
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
        Intent intent = new Intent();
        switch (wallet.type)
        {
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

    private void VerifySeedPhrase()
    {
        setContentView(R.layout.activity_verify_seed_phrase);
        initViews();

        if (layoutHolderWidth < 600)
        {
            spacerImage.setVisibility(View.GONE);
        }

        setTitle(getString(R.string.empty));
        state = BackupState.VERIFY_SEED_PHRASE;
        title.setText(R.string.verify_seed_phrase);
        nextButton.setText(R.string.action_continue);
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
        setContentView(R.layout.activity_backup_write_seed);
        initViews();

        setTitle(getString(R.string.empty));
        state = BackupState.WRITE_DOWN_SEED_PHRASE;
        title.setText(R.string.write_down_seed_phrase);
        nextButton.setText(R.string.wrote_down_seed_phrase);
        setBottomButtonActive(true);
    }

    private void DisplaySeed()
    {
        if (layoutWordHolder != null )
        {
            layoutWordHolder.setVisibility(View.VISIBLE);
            layoutWordHolder.removeAllViews();
        }

        viewModel.getSeedPhrase(wallet, this, this);
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

    private void setSeedWordSize(TextView seedWord)
    {
        seedWord.setTextSize(TypedValue.COMPLEX_UNIT_SP, seedTextSize);
        seedWord.setPadding(seedTextHPadding, seedTextVPadding, seedTextHPadding, seedTextVPadding);
//        if (layoutHolderWidth > 600)
//        {
//            seedWord.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
//            seedWord.setPadding(40, 25, 40, 25);
//        }
//        else
//        {
//            seedWord.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
//            seedWord.setPadding(16, 6, 16, 6);
//        }
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
        setSeedWordSize(seedWord);

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
            intent.putExtra("Key", wallet.address);
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
        intent.putExtra("Key", wallet.address);
        finish();
    }

    @Override
    public void FetchMnemonic(String mnemonic)
    {
        switch (state)
        {
            case WRITE_DOWN_SEED_PHRASE:
                WriteDownSeedPhrase();
                mnemonicArray = mnemonic.split(" ");
                addSeedWordsToScreen();
                break;
            case ENTER_JSON_BACKUP:
            case SET_JSON_PASSWORD:
                inputView = findViewById(R.id.input_password);
                viewModel.exportWallet(wallet, mnemonic, inputView.getText().toString());
                break;
            case SHOW_SEED_PHRASE:
                setupTestSeed();
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
    }

    private void addSeedWordsToScreen()
    {
        if (mnemonicArray == null) return;
        addNewLayoutLine();
        //start adding words to layout
        for (String word : mnemonicArray)
        {
            addWordToLayout(word);
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

        Operation taskCode = null;

        //Interpret the return code; if it's within the range of values possible to return from PIN confirmation then separate out
        //the task code from the return value. We have to do it this way because there's no way to send a bundle across the PIN dialog
        //and out through the PIN dialog's return back to here
        if (requestCode >= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS && requestCode <= SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS + 10)
        {
            taskCode = Operation.values()[requestCode - SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS];
            requestCode = SignTransactionDialog.REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS;
        }

        switch (requestCode)
        {
            case SHARE_REQUEST_CODE:
                if (resultCode == RESULT_OK)
                {
                    backupKeySuccess(BackupOperationType.BACKUP_KEYSTORE_KEY);
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
            case SHOW_SEED_PHRASE:
                break;
        }
    }

    private enum BackupState
    {
        UNDEFINED, ENTER_BACKUP_STATE_HD, WRITE_DOWN_SEED_PHRASE, VERIFY_SEED_PHRASE, SEED_PHRASE_INVALID,
        ENTER_JSON_BACKUP, SET_JSON_PASSWORD, SHOW_SEED_PHRASE, UPGRADE_KEY_SECURITY
    }

    public enum BackupOperationType
    {
        UNDEFINED, BACKUP_HD_KEY, BACKUP_KEYSTORE_KEY, SHOW_SEED_PHRASE, EXPORT_PRIVATE_KEY
    }

//                switch (operation)
//    {
//        case CREATE_HD_KEY:
//        case CREATE_NON_AUTHENTICATED_KEY:
//            if (callbackInterface != null)
//                callbackInterface.HDKeyCreated(address, context, authLevel);
//            break;
//        case IMPORT_HD_KEY:
//            importCallback.WalletValidated(address, authLevel);
//            deleteNonAuthKeyEncryptedKeyBytes(address); //in the case the user re-imported a key, destroy the backup key
//            break;
//        case UPGRADE_HD_KEY:
//        case UPGRADE_KEYSTORE_KEY:
//            signCallback.CreatedKey(address);
//            deleteNonAuthKeyEncryptedKeyBytes(address);
//            break;
//        case CREATE_KEYSTORE_KEY:
//            importCallback.KeystoreValidated(new String(data), authLevel);
//            break;
//        case CREATE_PRIVATE_KEY:
//            importCallback.KeyValidated(new String(data), authLevel);
//            break;
//        default:
//            break;
//    }

}
