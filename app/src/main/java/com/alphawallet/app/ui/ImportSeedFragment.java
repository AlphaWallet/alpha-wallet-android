package com.alphawallet.app.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.widget.OnImportSeedListener;
import com.alphawallet.app.ui.widget.OnSuggestionClickListener;
import com.alphawallet.app.ui.widget.adapter.SuggestionsAdapter;
import com.alphawallet.app.widget.LayoutCallbackListener;
import com.alphawallet.app.widget.PasswordInputView;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ImportSeedFragment extends Fragment implements View.OnClickListener, TextWatcher, LayoutCallbackListener, OnSuggestionClickListener {
    private static final OnImportSeedListener dummyOnImportSeedListener = (s, c) -> {};
    private static final String validator = "[^a-z^A-Z^ ]";
    private final int maxWordCount = 12;

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
    private int inputWords = 0;

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
        seedPhrase = getActivity().findViewById(R.id.input_seed);
        importButton = getActivity().findViewById(R.id.import_action);
        wordCount = getActivity().findViewById(R.id.text_word_count);
        listSuggestions = getActivity().findViewById(R.id.list_suggestions);
        nonEnglishHint = getActivity().findViewById(R.id.text_non_english_hint);
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
        String newMnemonic = seedPhrase.getText().toString();
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
    public void onTextChanged(CharSequence charSequence, int start, int count, int after)
    {
        deletePressed = count == 1;
    }

    @Override
    public void afterTextChanged(Editable editable)
    {
        if (seedPhrase.isErrorState()) seedPhrase.setError(null);
        String value = seedPhrase.getText().toString();
        final Matcher matcher = pattern.matcher(value);
        if (matcher.find())
        {
            seedPhrase.setError("Seed phrase can only contain words");
            wordCount.setVisibility(View.GONE);
        }
        else if (value.length() > 5)
        {
            wordCount.setVisibility(View.VISIBLE);
        }
        else
        {
            wordCount.setVisibility(View.VISIBLE);
        }

        inputWords = wordCount(value);
        String wordCountDisplay = inputWords + "/" + maxWordCount;
        wordCount.setText(wordCountDisplay);

        if (inputWords != maxWordCount && importButton != null && importButton.getVisibility() == View.VISIBLE)
        {
            importButton.setVisibility(View.GONE);
            setHintState(false);
        }

        if (inputWords == maxWordCount)
        {
            wordCount.setTextColor(ContextCompat.getColor(Objects.requireNonNull(getActivity()), R.color.nasty_green));
            wordCount.setTypeface(boldTypeface);
            updateButtonState(true);
        }
        else if (inputWords == (maxWordCount -1))
        {
            wordCount.setTextColor(ContextCompat.getColor(Objects.requireNonNull(getActivity()), R.color.colorPrimaryDark));
            wordCount.setTypeface(normalTypeface);
            updateButtonState(false);
        }
        else if (inputWords > maxWordCount)
        {
            wordCount.setTextColor(ContextCompat.getColor(Objects.requireNonNull(getActivity()), R.color.dark_seed_danger));
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
                else if (!(suggestionsAdapter.getSingleSuggestion().equals(lastWord) && inputWords == maxWordCount) && listSuggestions.getVisibility() == View.GONE)
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
        List<String> filteredList = Lists.newArrayList(Collections2.filter(suggestions, input -> input.startsWith(lastWord)));
        suggestionsAdapter.setData(filteredList, lastWord);
    }

    @Override
    public void onLayoutShrunk()
    {
        if (listSuggestions.getVisibility() == View.GONE && inputWords != maxWordCount)
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
        if (inputWords == maxWordCount)
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
        seedPhrase.getEditText().append(value + " ");
    }
}
