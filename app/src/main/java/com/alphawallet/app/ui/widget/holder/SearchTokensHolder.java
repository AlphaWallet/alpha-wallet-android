package com.alphawallet.app.ui.widget.holder;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alphawallet.app.R;
import com.alphawallet.app.ui.widget.TokensAdapterCallback;
import com.alphawallet.app.ui.widget.entity.ManageTokensData;

public class SearchTokensHolder extends BinderViewHolder<ManageTokensData> {
    public static final int VIEW_TYPE = 2021;

    public interface SearchHandler {
        void onFocus();
    }

    final EditText editSearch;
    final SearchHandler searchHandler;
    final SearchHandler onWCClicked;
    final View searchTokenClick;
    final ImageView walletConnect;
    String wallet;

    @Override
    public void bind(@Nullable ManageTokensData data, @NonNull Bundle addition) {
        if (wallet != null) return; //don't re-bind if view exists
        if (data != null) wallet = data.walletAddress;

        editSearch.setOnFocusChangeListener((v, hasFocus) -> {
            editSearch.clearFocus();
            if (hasFocus) searchHandler.onFocus();
        });

        searchTokenClick.setOnClickListener(v -> {
            if (searchHandler != null) searchHandler.onFocus();
        });
    }

    public SearchTokensHolder(int res_id, ViewGroup parent, TokensAdapterCallback tCallback) {
        super(res_id, parent);
        this.editSearch = findViewById(R.id.edit_search);
        this.searchHandler = tCallback::onSearchClicked;
        this.searchTokenClick = findViewById(R.id.click_layer);
        this.walletConnect = findViewById(R.id.icon_wc_active);
        this.wallet = null;
        this.onWCClicked = tCallback::onWCClicked;

        if (tCallback.hasWCSession())
        {
            enableWalletConnect();
        }
    }

    public void enableWalletConnect()
    {
        walletConnect.setVisibility(View.VISIBLE);
        walletConnect.setOnClickListener(v -> {
            if (onWCClicked != null) onWCClicked.onFocus();
        });
    }

    public void hideWalletConnect()
    {
        walletConnect.setVisibility(View.GONE);
        walletConnect.setOnClickListener(null);
    }
}
