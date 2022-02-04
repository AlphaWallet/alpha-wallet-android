package com.alphawallet.app.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.widget.OnImportPrivateKeyListener;
import com.alphawallet.app.widget.PasswordInputView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ImportPrivateKeyFragment extends ImportFragment
{
    private static final OnImportPrivateKeyListener dummyOnImportPrivateKeyListener = key -> { };
    private static final String validator = "[^0x^\\s^a-f^A-F^0-9]";
    private static final Pattern findKey = Pattern.compile("($|\\s?|0x?)([0-9a-fA-F]{64})($|\\s?)");

    private PasswordInputView privateKey;
    private Button importButton;
    private OnImportPrivateKeyListener onImportPrivateKeyListener = dummyOnImportPrivateKeyListener;
    private Pattern pattern;
    private LinearLayout buttonHolder;

    public static ImportPrivateKeyFragment create() {
        return new ImportPrivateKeyFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return LayoutInflater.from(getContext())
                .inflate(R.layout.fragment_import_private_key, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupView();
    }

    private void setupView()
    {
        privateKey = getView().findViewById(R.id.input_private_key);
        importButton = getView().findViewById(R.id.import_action_pk);
        buttonHolder = getView().findViewById(R.id.button_holder_pk);
        importButton.setOnClickListener(this);
        privateKey.getEditText().addTextChangedListener(this);
        updateButtonState(false);
        pattern = Pattern.compile(validator, Pattern.MULTILINE);

        privateKey.setLayoutListener(getActivity(), this);
    }

    @Override
    public void onClick(View view) {
        handleKey(view);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (privateKey == null && getActivity() != null) setupView();
    }

    private void handleKey(View view)
    {
        privateKey.setError(null);
        String value = privateKey.getText().toString();

        if (!TextUtils.isEmpty(value))
        {
            final Matcher matcher = findKey.matcher(value);
            if (matcher.find())
            {
                value = matcher.group(2);
            }

            //value = Numeric.cleanHexPrefix(value.replaceAll("\\s+", "")); //remove whitespace and leading 0x
            if (value.length() == 64)
            {
                privateKey.setText(value);
                onImportPrivateKeyListener.onPrivateKey(value);
            }
            else
            {
                privateKey.setError(getString(R.string.suggestion_private_key));
            }
        }
        else
        {
            privateKey.setError(getString(R.string.error_field_required));
        }
    }

    private void updateButtonState(boolean enabled)
    {
        importButton.setEnabled(enabled);
    }

    public void setOnImportPrivateKeyListener(OnImportPrivateKeyListener onImportPrivateKeyListener) {
        this.onImportPrivateKeyListener = onImportPrivateKeyListener == null
                ? dummyOnImportPrivateKeyListener
                : onImportPrivateKeyListener;
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
        if (privateKey.isErrorState()) privateKey.setError(null);
        String value = privateKey.getText().toString();
        final Matcher matcher = pattern.matcher(value);
        if (matcher.find())
        {
            updateButtonState(false);
            privateKey.setError(R.string.private_key_check);
            return;
        }

        final Matcher privateKeyMatch = findKey.matcher(value);
        updateButtonState(privateKeyMatch.find());
    }

    public String getPrivateKey()
    {
        String value = privateKey.getText().toString();
        final Matcher matcher = findKey.matcher(value);
        if (matcher.find())
        {
            return matcher.group(2);
        }
        else
        {
            return "";
        }
    }

    @Override
    public void onLayoutShrunk()
    {
        if (buttonHolder != null) buttonHolder.setVisibility(View.GONE);
    }

    @Override
    public void onLayoutExpand()
    {
        if (buttonHolder != null) buttonHolder.setVisibility(View.VISIBLE);
    }

    @Override
    public void onInputDoneClick(View view)
    {
        handleKey(view);
    }
}
