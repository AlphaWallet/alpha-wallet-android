package com.alphawallet.app.ui;

import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.widget.OnImportKeystoreListener;
import com.alphawallet.app.widget.PasswordInputView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ImportKeystoreFragment extends ImportFragment
{
    private static final OnImportKeystoreListener dummyOnImportKeystoreListener = (k, p) -> {};
    private static final Pattern keystore_json = Pattern.compile("($|\\s?)(\\{)([\\n\\r\\t\" a-zA-Z0-9{}:,-]{12,})(\\})($|\\s?)", Pattern.MULTILINE);

    private PasswordInputView keystore;
    private PasswordInputView password;
    private Button importButton;
    private TextView passwordText;
    private TextView importText;

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

        setupView();
    }

    private void setupView()
    {
        keystore = getView().findViewById(R.id.input_keystore);
        password = getView().findViewById(R.id.input_password);
        passwordText = getView().findViewById(R.id.text_password_notice);
        importText = getView().findViewById(R.id.import_text);
        passwordText.setVisibility(View.GONE);
        password.setVisibility(View.GONE);
        importButton = getView().findViewById(R.id.import_action_ks);
        importButton.setOnClickListener(this);
        updateButtonState(false);
        keystore.getEditText().addTextChangedListener(this);
        password.getEditText().addTextChangedListener(this);

        keystore.setLayoutListener(getActivity(), this);
        password.setLayoutListener(getActivity(), this);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (keystore == null && getActivity() != null) setupView();
    }

    @Override
    public void comeIntoFocus()
    {
        reset();
    }

    private void updateButtonState(boolean enabled)
    {
        importButton.setEnabled(enabled);
    }

    private void handleKeypress(View view)
    {
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

    @Override
    public void onClick(View view) {
        handleKeypress(view);
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
        {  // currently user entering keystore JSON
            String keyStoreEntry = keystore.getText().toString();
            if (keyStoreEntry.length() > 0)
            {
                final Matcher pattern = keystore_json.matcher(keyStoreEntry);
                keystore.setError(pattern.matches() ? null : getString(R.string.invalid_keystore));
                updateButtonState(pattern.matches());
            }
        }
        else
        {
            String txt = password.getText().toString();
            updateButtonState(txt.length() >= 1);
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

    @Override
    public void onLayoutShrunk()
    {
        if (importText != null) importText.setVisibility(View.GONE);
        if (importButton != null) importButton.setVisibility(View.GONE);
    }

    @Override
    public void onLayoutExpand()
    {
        if (importText != null) importText.setVisibility(View.VISIBLE);
        if (importButton != null) importButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onInputDoneClick(View view)
    {
        handleKeypress(view);
    }
}
