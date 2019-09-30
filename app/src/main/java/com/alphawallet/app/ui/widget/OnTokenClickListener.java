package com.alphawallet.app.ui.widget;

import android.view.View;
import com.alphawallet.app.entity.Token;

import java.math.BigInteger;
import java.util.List;

public interface OnTokenClickListener {
    void onTokenClick(View view, Token token, List<BigInteger> tokenIds, boolean selected);
    void onLongTokenClick(View view, Token token, List<BigInteger> tokenIds);
}
