package com.alphawallet.app.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.widget.OnImportKeystoreListener;
import com.alphawallet.app.widget.LayoutCallbackListener;
import com.alphawallet.app.widget.PasswordInputView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ImportKeystoreFragment extends Fragment implements View.OnClickListener, TextWatcher, LayoutCallbackListener
{
    private static final OnImportKeystoreListener dummyOnImportKeystoreListener = (k, p) -> {};
    private static final Pattern keystore_json = Pattern.compile("[\\n\\r\\t\" a-zA-Z0-9{}:,-]{10,}", Pattern.MULTILINE);

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
        keystore = getActivity().findViewById(R.id.input_keystore);
        password = getActivity().findViewById(R.id.input_password);
        passwordText = getActivity().findViewById(R.id.text_password_notice);
        importText = getActivity().findViewById(R.id.import_text);
        passwordText.setVisibility(View.GONE);
        password.setVisibility(View.GONE);
        importButton = getActivity().findViewById(R.id.import_action_ks);
        importButton.setOnClickListener(this);
        updateButtonState(false);
        keystore.getEditText().addTextChangedListener(this);
        password.getEditText().addTextChangedListener(this);

        keystore.setLayoutListener(getActivity(), this, getActivity().findViewById(R.id.bottom_marker_ks));
        password.setLayoutListener(getActivity(), this, getActivity().findViewById(R.id.bottom_marker_ks));
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (keystore == null && getActivity() != null) setupView();
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
