package com.alphawallet.app.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.widget.OnImportSeedListener;
import com.alphawallet.app.ui.widget.OnSuggestionClickListener;
import com.alphawallet.app.ui.widget.adapter.SuggestionsAdapter;
import com.alphawallet.app.viewmodel.PasswordPhraseCounter;
import com.alphawallet.app.widget.PasswordInputView;
import com.google.common.collect.Collections2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ImportSeedFragment extends ImportFragment implements OnSuggestionClickListener {
    private static final OnImportSeedListener dummyOnImportSeedListener = (s, c) -> {};

    public static final String validator = "[^a-z^A-Z^ ]";

    private PasswordInputView seedPhrase;
    private Button importButton;
    private Pattern pattern;
    private TextView wordCount;
    private TextView nonEnglishHint;
    private RecyclerView listSuggestions;
    private List<String> suggestions;
    private SuggestionsAdapter suggestionsAdapter;
    Typeface boldTypeface = Typeface.defaultFromStyle(Typeface.BOLD);
    Typeface normalTypeface = Typeface.defaultFromStyle(Typeface.NORMAL);
    private boolean deletePressed;

    @NonNull
    private OnImportSeedListener onImportSeedListener = dummyOnImportSeedListener;
    private PasswordPhraseCounter passwordPhraseCounter;

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
        setHintState(true);
    }

    private void setupView()
    {
        seedPhrase = getView().findViewById(R.id.input_seed);
        importButton = getView().findViewById(R.id.import_action);
        wordCount = getView().findViewById(R.id.text_word_count);
        listSuggestions = getView().findViewById(R.id.list_suggestions);
        nonEnglishHint = getView().findViewById(R.id.text_non_english_hint);
        importButton.setOnClickListener(this);
        seedPhrase.getEditText().addTextChangedListener(this);
        updateButtonState(false);
        pattern = Pattern.compile(validator, Pattern.MULTILINE);
        wordCount.setVisibility(View.VISIBLE);

        seedPhrase.setLayoutListener(getActivity(), this);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        listSuggestions.setLayoutManager(linearLayoutManager);

        suggestions = Arrays.asList(getResources().getStringArray(R.array.bip39_english));
        suggestionsAdapter = new SuggestionsAdapter(suggestions, this);
        listSuggestions.setAdapter(suggestionsAdapter);
    }

    private void setHintState(boolean enabled){
        String lang = Locale.getDefault().getDisplayLanguage();
        if (nonEnglishHint == null) return;
        if (enabled && !lang.equalsIgnoreCase("English")) //remove language hint for English locale
        {
            nonEnglishHint.setVisibility(View.VISIBLE);
        }
        else
        {
            nonEnglishHint.setVisibility(View.GONE);
        }
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
        String newMnemonic = seedPhrase.getText().toString().toLowerCase(Locale.ENGLISH);
        seedPhrase.getEditText().setText("");
        seedPhrase.getEditText().append(newMnemonic);
        if (TextUtils.isEmpty(newMnemonic)) {
            this.seedPhrase.setError(getString(R.string.error_field_required));
        } else {
            String[] words = newMnemonic.split("\\s+");
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String word : words)
            {
                if (!first) sb.append(" ");
                sb.append(word);
                first = false;
            }
            onImportSeedListener.onSeed(sb.toString(), getActivity());
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
        importButton.setEnabled(enabled);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
    {

    }

    @Override
    public void onTextChanged(CharSequence charSequence, int start, int count, int after)
    {
        deletePressed = count == 1;
    }

    @Override
    public void afterTextChanged(Editable editable)
    {
        String value = seedPhrase.getText().toString().toLowerCase(Locale.ENGLISH);
        passwordPhraseCounter = new PasswordPhraseCounter(wordCount(value));

        if (seedPhrase.isErrorState()) seedPhrase.setError(null);
        final Matcher matcher = pattern.matcher(value);
        if (matcher.find())
        {
            seedPhrase.setError(getString(R.string.error_seed_phrase_must_words));
            wordCount.setVisibility(View.GONE);
        }
        else
        {
            wordCount.setVisibility(View.VISIBLE);
        }

        wordCount.setText(passwordPhraseCounter.getText());

        if (!passwordPhraseCounter.match() && importButton != null && importButton.getVisibility() == View.VISIBLE)
        {
            importButton.setVisibility(View.GONE);
            setHintState(false);
        }

        if (passwordPhraseCounter.match()) {
            wordCount.setTextColor(ContextCompat.getColor(requireActivity(), R.color.positive));
            wordCount.setTypeface(boldTypeface);
            updateButtonState(true);
        } else if (passwordPhraseCounter.notEnough())
        {
            wordCount.setTextColor(ContextCompat.getColor(requireActivity(), R.color.text_secondary));
            wordCount.setTypeface(normalTypeface);
            updateButtonState(false);
        } else if (passwordPhraseCounter.exceed())
        {
            wordCount.setTextColor(ContextCompat.getColor(requireActivity(), R.color.error));
            updateButtonState(false);
        }

        //get last word from the text
        if (value.length() > 0)
        {
            String lastWord = getLastWord(value);
            if (lastWord.trim().length() > 0)
            {
                filterList(lastWord);
                if (!deletePressed && suggestionsAdapter.getSingleSuggestion().equals(lastWord))
                {
                    seedPhrase.getEditText().append(" ");
                    listSuggestions.setVisibility(View.GONE);
                    showImport();
                }
                else if (!(suggestionsAdapter.getSingleSuggestion().equals(lastWord) && passwordPhraseCounter.match()) && listSuggestions.getVisibility() == View.GONE)
                {
                    listSuggestions.setVisibility(View.VISIBLE);
                    importButton.setVisibility(View.GONE);
                }
            }
            else
            {
                listSuggestions.setVisibility(View.GONE);
                showImport();
            }
        }
        else
        {
            listSuggestions.setVisibility(View.GONE);
            showImport();
        }
    }

    private int wordCount(String value)
    {
        if (value == null || value.length() == 0) return 0;
        String[] split = value.split("\\s+");
        return split.length;
    }

    private String getLastWord(String value)
    {
        int lastDelimiterPosition = value.lastIndexOf(" ");
        return lastDelimiterPosition == -1 ? value :
                          value.substring(lastDelimiterPosition + " ".length());
    }

    private void filterList(String lastWord) {
        List<String> filteredList = new ArrayList<String>(Collections2.filter(suggestions, input -> input.startsWith(lastWord)));
        suggestionsAdapter.setData(filteredList, lastWord);
    }

    @Override
    public void onLayoutShrunk()
    {
        if (listSuggestions.getVisibility() == View.GONE && passwordPhraseCounter != null && !passwordPhraseCounter.match())
        {
            if (importButton != null) importButton.setVisibility(View.GONE);
            setHintState(false);
        }
    }

    @Override
    public void onLayoutExpand()
    {
        if (importButton != null) importButton.setVisibility(View.VISIBLE);
        listSuggestions.setVisibility(View.GONE);
        setHintState(true);
    }

    private void showImport()
    {
        if (passwordPhraseCounter.match())
        {
            if (importButton != null) importButton.setVisibility(View.VISIBLE);
            setHintState(true);
        }
    }

    @Override
    public void onInputDoneClick(View view)
    {
        processSeed(view);
    }

    @Override
    public void onSuggestionClick(String value)
    {
        StringBuilder seed = new StringBuilder(seedPhrase.getEditText().getText().toString());
        seed.append(value);
        seed.append(" ");
        seedPhrase.getEditText().setText("");
        seedPhrase.getEditText().append(seed.toString().toLowerCase(Locale.ENGLISH));
    }
}
