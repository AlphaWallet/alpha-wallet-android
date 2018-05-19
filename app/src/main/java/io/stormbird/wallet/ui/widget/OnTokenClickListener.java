package io.stormbird.wallet.ui.widget;

import android.view.View;

import io.stormbird.wallet.entity.Token;

public interface OnTokenClickListener {
    void onTokenClick(View view, Token token);
}
