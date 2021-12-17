package com.alphawallet.app.ui.widget;

import android.view.View;

import com.alphawallet.app.entity.tokens.Token;

import java.math.BigInteger;
import java.util.List;

public interface TokensAdapterCallback
{
    void onTokenClick(View view, Token token, List<BigInteger> tokenIds, boolean selected);
    void onLongTokenClick(View view, Token token, List<BigInteger> tokenIds);
    default void reloadTokens() { };
    default void onBuyToken() { }
    default void onSearchClicked() { };
}
