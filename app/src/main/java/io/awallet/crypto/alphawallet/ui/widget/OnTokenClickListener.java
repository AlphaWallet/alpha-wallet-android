package io.awallet.crypto.alphawallet.ui.widget;

import android.view.View;

import io.awallet.crypto.alphawallet.entity.Token;

public interface OnTokenClickListener {
    void onTokenClick(View view, Token token);
}
