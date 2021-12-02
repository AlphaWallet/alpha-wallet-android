package com.alphawallet.app.ui.widget.holder;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.widget.entity.ManageTokensData;
import com.alphawallet.app.util.KeyboardUtils;

public class SearchTokensHolder extends BinderViewHolder<ManageTokensData> {
    public static final int VIEW_TYPE = 2021;

    public interface SearchHandler {
        void onSearch(String filter, boolean skipDebounce);
    }

    EditText editSearch;
    SearchHandler searchHandler;
    String wallet;

    TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable s) {
            searchHandler.onSearch(s.toString(), false);
        }
    };

    @Override
    public void bind(@Nullable ManageTokensData data, @NonNull Bundle addition) {
        if (wallet != null) return; //don't re-bind if view exists
        if (data != null) wallet = data.walletAddress;
        editSearch.removeTextChangedListener(textWatcher);
        editSearch.addTextChangedListener(textWatcher);

        //Closes the keyboard if we scroll down - uncovers potentially blocked views
        editSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) { KeyboardUtils.hideKeyboard(v); }
        });

        editSearch.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchHandler.onSearch(textView.getText().toString(), true);
                textView.clearFocus();
                KeyboardUtils.hideKeyboard(textView);
            }
            return actionId == EditorInfo.IME_ACTION_SEARCH;
        });
    }

    public SearchTokensHolder(int res_id, ViewGroup parent, SearchHandler handler) {
        super(res_id, parent);
        this.editSearch = findViewById(R.id.edit_search);
        this.searchHandler = handler;
        this.wallet = null;
    }
}
