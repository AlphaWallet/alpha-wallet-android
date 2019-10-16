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
import android.widget.TextView;

import com.alphawallet.app.ui.widget.OnImportSeedListener;

import com.alphawallet.app.R;

import com.alphawallet.app.widget.LayoutCallbackListener;
import com.alphawallet.app.widget.PasswordInputView;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ImportSeedFragment extends Fragment implements View.OnClickListener, TextWatcher, LayoutCallbackListener
{
    private static final OnImportSeedListener dummyOnImportSeedListener = (s, c) -> {};
    private static final String validator = "[^a-z^A-Z^ ]";

    private PasswordInputView seedPhrase;
    private Button importButton;
    private Pattern pattern;
    @NonNull
    private OnImportSeedListener onImportSeedListener = dummyOnImportSeedListener;

    public static ImportSeedFragment create() {
        return new ImportSeedFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return LayoutInflater.from(getContext())
                .inflate(R.layout.fragment_import_seed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupView();
    }

    private void setupView()
    {
        seedPhrase = getActivity().findViewById(R.id.input_seed);
        importButton = getActivity().findViewById(R.id.import_action);
        importButton.setOnClickListener(this);
        seedPhrase.getEditText().addTextChangedListener(this);
        updateButtonState(false);
        pattern = Pattern.compile(validator, Pattern.MULTILINE);

        String lang = Locale.getDefault().getDisplayLanguage();
        if (lang.equalsIgnoreCase("English")) //remove language hint for English locale
        {
            getActivity().findViewById(R.id.text_non_english_hint).setVisibility(View.GONE);
        }

        seedPhrase.setLayoutListener(getActivity(), this, getActivity().findViewById(R.id.bottom_marker));
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (seedPhrase == null && getActivity() != null) setupView();
    }

    @Override
    public void onClick(View view) {
        processSeed(view);
    }

    private void processSeed(View view)
    {
        this.seedPhrase.setError(null);
        String newMnemonic = seedPhrase.getText().toString();
        if (TextUtils.isEmpty(newMnemonic)) {
            this.seedPhrase.setError(getString(R.string.error_field_required));
        } else {
            onImportSeedListener.onSeed(newMnemonic, getActivity());
        }
    }

    public void setOnImportSeedListener(@Nullable OnImportSeedListener onImportSeedListener) {
        this.onImportSeedListener = onImportSeedListener == null
                ? dummyOnImportSeedListener
                : onImportSeedListener;
    }

    public void onBadSeed()
    {
        seedPhrase.setError(R.string.bad_seed_phrase);
    }

    private void updateButtonState(boolean enabled)
    {
        importButton.setActivated(enabled);
        importButton.setClickable(enabled);
        int colorId = enabled ? R.color.nasty_green : R.color.inactive_green;
        if (getContext() != null) importButton.setBackgroundColor(getContext().getColor(colorId));
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
        if (seedPhrase.isErrorState()) seedPhrase.setError(null);
        String value = seedPhrase.getText().toString();
        final Matcher matcher = pattern.matcher(value);
        if (matcher.find())
        {
            updateButtonState(false);
            seedPhrase.setError("Seed phrase can only contain words");
        }
        else if (value.length() > 5)
        {
            updateButtonState(true);
        }
        else
        {
            updateButtonState(false);
        }
    }

    @Override
    public void onLayoutShrunk()
    {
        if (importButton != null) importButton.setVisibility(View.GONE);
    }

    @Override
    public void onLayoutExpand()
    {
        if (importButton != null) importButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onInputDoneClick(View view)
    {
        processSeed(view);
    }
}
