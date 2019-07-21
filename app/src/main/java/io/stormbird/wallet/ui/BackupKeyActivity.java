package io.stormbird.wallet.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import dagger.android.AndroidInjection;
import io.stormbird.wallet.C;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.CreateWalletCallbackInterface;
import io.stormbird.wallet.entity.PinAuthenticationCallbackInterface;
import io.stormbird.wallet.service.HDKeyService;
import io.stormbird.wallet.util.KeyboardUtils;
import io.stormbird.wallet.viewmodel.BackupKeyViewModel;
import io.stormbird.wallet.viewmodel.BackupKeyViewModelFactory;
import io.stormbird.wallet.widget.PasswordInputView;
import io.stormbird.wallet.widget.ProgressView;
import io.stormbird.wallet.widget.SignTransactionDialog;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static io.stormbird.wallet.C.SHARE_REQUEST_CODE;

public class BackupKeyActivity extends BaseActivity implements View.OnClickListener, CreateWalletCallbackInterface
{
    @Inject
    BackupKeyViewModelFactory backupKeyViewModelFactory;
    BackupKeyViewModel viewModel;

    private BackupState state;
    private String keyBackup;
    private TextView title;
    private TextView detail;
    private TextView passwordDetail;
    private LinearLayout layoutHolder;
    private LinearLayout layoutWordHolder;
    private PasswordInputView inputView;
    private ImageView backupImage;
    private Button nextButton;
    private TextView verifyTextBox;
    private String[] mnemonicArray;
    private PinAuthenticationCallbackInterface authInterface;

    private LinearLayout currentHolder;
    private int currentEdge;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_backup_seed);

        toolbar();
        ProgressView progressView = findViewById(R.id.progress_view);
        progressView.hide();

        String type = getIntent().getStringExtra("TYPE");
        keyBackup = getIntent().getStringExtra("ADDRESS");
        initViews();

        switch (type)
        {
            case "HDKEY":
                state = BackupState.ENTER_BACKUP_STATE_HD;
                setTitle(R.string.backup_seed_phrase);
                break;
            case "JSON":
                state = BackupState.ENTER_JSON_BACKUP;
                setTitle(getString(R.string.export_keystore_json));
                setupJSONExport();
                break;
            case "TEST_SEED":
                state = BackupState.TEST_SEED_PHRASE;
                setTitle(getString(R.string.seed_phrase));
                setupTestSeed();
                break;
        }

        initViewModel();
    }

    private void setupJSONExport()
    {
        title.setText(R.string.export_keystore_json);
        backupImage.setImageResource(R.drawable.json_graphic);
        detail.setVisibility(View.VISIBLE);
        detail.setText(R.string.keystore_detail_text);
        nextButton.setText(R.string.export_keystore_json);
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
    }

    @Override
    public void onResume()
    {
        super.onResume();
        initViews();
    }

    private void initViews()
    {
        title = findViewById(R.id.text_title);
        detail = findViewById(R.id.text_detail);
        passwordDetail = findViewById(R.id.text_detail_password);
        layoutHolder = findViewById(R.id.layout_center_holder);
        layoutWordHolder = findViewById(R.id.layout_word_holder);
        nextButton = findViewById(R.id.button_next);
        verifyTextBox = findViewById(R.id.text_verify);
        backupImage = findViewById(R.id.seed_image);
        inputView = findViewById(R.id.input_password);
        nextButton.setOnClickListener(this);
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
                    default:
                        tryAgain();
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
                ShareEncryptedKeystore();
                break;
            case TEST_SEED_PHRASE:
                VerifySeedPhrase();
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

    private void ShareEncryptedKeystore()
    {
        viewModel.exportWallet(keyBackup, inputView.getText().toString());
        KeyboardUtils.hideKeyboard(inputView);
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
        nextButton.setText(R.string.share_keystore);
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

        //terminate and display tick
        backupTestPassed();
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

    private void backupTestPassed()
    {
        Intent intent = new Intent();
        intent.putExtra("Key", keyBackup);
        intent.putExtra("TYPE", "HDKEY");
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

        //fetch Mnemonic
        HDKeyService svs = new HDKeyService(this);
        svs.getMnemonic(keyBackup, this);
    }

    private void addNewLayoutLine()
    {
        currentEdge = 0;
        currentHolder = new LinearLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        params.setMargins(0,0,0,8);
        currentHolder.setLayoutParams(params);
        currentHolder.setOrientation(LinearLayout.HORIZONTAL);
        layoutWordHolder.addView(currentHolder);
    }

    private TextView addWordToLayout(String word)
    {
        int width = layoutHolder.getWidth();

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
        if ((currentEdge + measuredWidth + 8) > width)
        {
            addNewLayoutLine();
        }

        currentEdge += (measuredWidth + 8);

        currentHolder.addView(seedWord);
        return seedWord;
    }

    @Override
    public void HDKeyCreated(String address, Context ctx)
    {
        //empty, doesn't get called
    }

    @Override
    public void tryAgain()
    {
        //TODO: Error trying to retrieve the key, display error dialog box here
        Intent intent = new Intent();
        setResult(RESULT_CANCELED, intent);
        finish();
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
        addNewLayoutLine();
        mnemonicArray = mnemonic.split(" ");
        //start adding words to layout
        for (String word : mnemonicArray)
        {
            addWordToLayout(word);
        }
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
                Intent intent = new Intent();
                intent.putExtra("Key", keyBackup);
                intent.putExtra("TYPE", "JSON");
                setResult(resultCode, intent);
                finish();
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

    private enum BackupState
    {
        ENTER_BACKUP_STATE_HD, WRITE_DOWN_SEED_PHRASE, VERIFY_SEED_PHRASE, SEED_PHRASE_INVALID,
        ENTER_JSON_BACKUP, SET_JSON_PASSWORD, TEST_SEED_PHRASE
    }
}
