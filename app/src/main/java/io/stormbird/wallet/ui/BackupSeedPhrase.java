package io.stormbird.wallet.ui;

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
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.CreateWalletCallbackInterface;
import io.stormbird.wallet.service.HDKeyService;
import io.stormbird.wallet.widget.ProgressView;

import java.util.ArrayList;
import java.util.List;

public class BackupSeedPhrase extends BaseActivity implements View.OnClickListener, CreateWalletCallbackInterface
{
    private BackupState state;
    private String keyBackup;
    private TextView title;
    private TextView detail;
    private LinearLayout layoutHolder;
    private LinearLayout layoutWordHolder;
    private Button nextButton;
    private TextView verifyTextBox;
    private String[] mnemonicArray;

    private LinearLayout currentHolder;
    private int currentEdge;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_backup_seed);

        toolbar();
        ProgressView progressView = findViewById(R.id.progress_view);
        progressView.hide();

        String type = getIntent().getStringExtra("TYPE");
        keyBackup = getIntent().getStringExtra("ADDRESS");
        switch (type)
        {
            case "HDKEY":
                state = BackupState.ENTER_BACKUP_STATE_HD;
                break;
            case "JSON":
                state = BackupState.ENTER_JSON_BACKUP;
                break;
        }

        setTitle(R.string.backup_seed_phrase);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        //hide seed phrase and any visible words
        mnemonicArray = null;
        layoutWordHolder.setVisibility(View.GONE);
        layoutWordHolder.removeAllViews();
        verifyTextBox.setText("");
    }

    @Override
    public void onResume()
    {
        super.onResume();
        title = findViewById(R.id.text_title);
        detail = findViewById(R.id.text_detail);
        layoutHolder = findViewById(R.id.layout_center_holder);
        layoutWordHolder = findViewById(R.id.layout_word_holder);
        nextButton = findViewById(R.id.button_next);
        verifyTextBox = findViewById(R.id.text_verify);
        nextButton.setOnClickListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //16908332
        switch (item.getItemId()) {
            case android.R.id.home: {
                tryAgain();
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
            case ENTER_JSON_BACKUP:
                break;
            case SET_JSON_PASSWORD:
                break;
        }
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
    }

    private void backupTestPassed()
    {
        HDKeyService.flagAsBackedUp(this, keyBackup);
        Intent intent = new Intent();
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
        nextButton.setClickable(false);
        nextButton.setActivated(false);
        nextButton.setBackgroundColor(getColor(R.color.inactive_green));

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
            nextButton.setClickable(true);
            nextButton.setActivated(true);
            nextButton.setBackgroundColor(getColor(R.color.nasty_green));
        }
    }

    private void WriteDownSeedPhrase()
    {
        state = BackupState.WRITE_DOWN_SEED_PHRASE;
        ImageView backupImage = findViewById(R.id.seed_image);
        backupImage.setVisibility(View.GONE);
        title.setText(R.string.write_down_seed_phrase);
        detail.setVisibility(View.GONE);
        nextButton.setText(R.string.wrote_down_seed_phrase);
        layoutWordHolder.setVisibility(View.VISIBLE);
        layoutWordHolder.removeAllViews();

        addNewLayoutLine();

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
        mnemonicArray = mnemonic.split(" ");
        //start adding words to layout
        for (String word : mnemonicArray)
        {
            addWordToLayout(word);
        }
    }

    private enum BackupState
    {
        ENTER_BACKUP_STATE_HD, WRITE_DOWN_SEED_PHRASE, VERIFY_SEED_PHRASE,
        ENTER_JSON_BACKUP, SET_JSON_PASSWORD
    }
}
