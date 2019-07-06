package io.stormbird.wallet.ui.widget;

import android.view.View;
import io.stormbird.wallet.entity.Token;

import java.math.BigInteger;
import java.util.List;

public interface OnTokenClickListener {
    void onTokenClick(View view, Token token, List<BigInteger> tokenIds, boolean selected);
    void onLongTokenClick(View view, Token token, List<BigInteger> tokenIds);
}
