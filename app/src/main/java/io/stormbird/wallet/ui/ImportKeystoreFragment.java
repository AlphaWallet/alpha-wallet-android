package io.stormbird.wallet.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.TextView;
import io.stormbird.wallet.R;
import io.stormbird.wallet.ui.widget.OnImportKeystoreListener;
import io.stormbird.wallet.widget.InputView;
import io.stormbird.wallet.widget.PasswordInputView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ImportKeystoreFragment extends Fragment implements View.OnClickListener, TextWatcher
{
    private static final OnImportKeystoreListener dummyOnImportKeystoreListener = (k, p) -> {};
    private static final Pattern keystore_json = Pattern.compile("[\\n\\r\\t\" a-zA-Z0-9{}:,-]{10,}", Pattern.MULTILINE);

    private PasswordInputView keystore;
    private PasswordInputView password;
    private Button importButton;
    private TextView passwordText;
    @NonNull
    private OnImportKeystoreListener onImportKeystoreListener = dummyOnImportKeystoreListener;

    public static ImportKeystoreFragment create() {
        return new ImportKeystoreFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return LayoutInflater.from(getContext())
                .inflate(R.layout.fragment_import_keystore, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        keystore = view.findViewById(R.id.input_keystore);
        password = view.findViewById(R.id.input_password);
        passwordText = view.findViewById(R.id.text_password_notice);
        passwordText.setVisibility(View.GONE);
        password.setVisibility(View.GONE);
        importButton = view.findViewById(R.id.import_action);
        importButton.setOnClickListener(this);
        updateButtonState(false);
        keystore.getEditText().addTextChangedListener(this);
        password.getEditText().addTextChangedListener(this);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isResumed())
        {
            if (isVisibleToUser) reset();
        }
    }

    private void updateButtonState(boolean enabled)
    {
        importButton.setActivated(enabled);
        importButton.setClickable(enabled);
        int colorId = enabled ? R.color.nasty_green : R.color.inactive_green;
        if (getContext() != null) importButton.setBackgroundColor(getContext().getColor(colorId));
    }

    @Override
    public void onClick(View view) {
        if (password.getVisibility() == View.GONE)
        {
            keystore.setVisibility(View.GONE);
            password.setVisibility(View.VISIBLE);
            passwordText.setVisibility(View.VISIBLE);
            password.requestFocus();
            updateButtonState(false);
        }
        else
        {
            String keystore = this.keystore.getText().toString();
            String password = this.password.getText().toString();
            onImportKeystoreListener.onKeystore(keystore, password);
        }
    }

    public String getKeystore()
    {
        return this.keystore.getText().toString();
    }
    public String getPassword()
    {
        return this.password.getText().toString();
    }

    public boolean backPressed()
    {
        if (password != null && password.getVisibility() == View.VISIBLE)
        {
            keystore.setVisibility(View.VISIBLE);
            password.setVisibility(View.GONE);
            passwordText.setVisibility(View.GONE);
            keystore.requestFocus();
            updateButtonState(true);
            return true;
        }
        else
        {
            return false;
        }
    }

    public void setOnImportKeystoreListener(@Nullable OnImportKeystoreListener onImportKeystoreListener) {
        this.onImportKeystoreListener = onImportKeystoreListener == null
            ? dummyOnImportKeystoreListener
            : onImportKeystoreListener;
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
        if (keystore.isErrorState()) keystore.setError(null);
        if (password.isErrorState()) password.setError(null);
        if (password.getVisibility() == View.GONE)
        { // currently user entering keystore JSON
            final Matcher pattern = keystore_json.matcher(keystore.getText().toString());
            keystore.setError(pattern.matches()?null:getString(R.string.invalid_keystore));
            updateButtonState(pattern.matches());
        }
        else
        {
            String txt = password.getText().toString();
            if (txt.length() >= 1)
            {
                updateButtonState(true);
            }
            else
            {
                updateButtonState(false);
            }
        }
    }

    public void reset()
    {
        if (password != null)
        {
            password.setText("");
            password.setVisibility(View.GONE);
            passwordText.setVisibility(View.GONE);
        }
        if (keystore != null)
        {
            keystore.setVisibility(View.VISIBLE);
            keystore.setError(null);
            keystore.setText("");
        }
        updateButtonState(false);
    }
}
