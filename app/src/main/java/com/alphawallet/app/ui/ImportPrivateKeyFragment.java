package com.alphawallet.app.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.alphawallet.app.ui.widget.OnImportPrivateKeyListener;

import com.alphawallet.app.widget.LayoutCallbackListener;
import com.alphawallet.token.tools.Numeric;
import com.alphawallet.app.R;

import com.alphawallet.app.widget.PasswordInputView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImportPrivateKeyFragment extends Fragment implements View.OnClickListener, TextWatcher, LayoutCallbackListener
{
    private static final OnImportPrivateKeyListener dummyOnImportPrivateKeyListener = key -> { };
    private static final String validator = "[^a-f^A-F^0-9]";

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
        privateKey = getActivity().findViewById(R.id.input_private_key);
        importButton = getActivity().findViewById(R.id.import_action_pk);
        buttonHolder = getActivity().findViewById(R.id.button_holder_pk);
        importButton.setOnClickListener(this);
        privateKey.getEditText().addTextChangedListener(this);
        updateButtonState(false);
        pattern = Pattern.compile(validator, Pattern.MULTILINE);

        privateKey.setLayoutListener(getActivity(), this, getActivity().findViewById(R.id.bottom_marker_pk));
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
            value = Numeric.cleanHexPrefix(value.replaceAll("\\s+", "")); //remove whitespace and leading 0x
            if (value.length() == 64)
            {
                onImportPrivateKeyListener.onPrivateKey(value);
                return;
            }
            else
            {
                privateKey.setError(getString(R.string.suggestion_private_key));
                return;
            }
        }

        privateKey.setError(getString(R.string.error_field_required));
    }

    private void updateButtonState(boolean enabled)
    {
        importButton.setActivated(enabled);
        importButton.setClickable(enabled);
        int colorId = enabled ? R.color.nasty_green : R.color.inactive_green;
        if (getContext() != null) importButton.setBackgroundColor(getContext().getColor(colorId));
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
        }
        else if (value.length() > 10)
        {
            updateButtonState(true);
        }
        else
        {
            updateButtonState(false);
        }
    }

    public String getPrivateKey()
    {
        String value = privateKey.getText().toString();
        return Numeric.cleanHexPrefix(value.replaceAll("\\s+", "")); //remove whitespace and leading 0x
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
