package com.wallet.crypto.alphawallet.ui.widget.holder;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.Token;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Created by James on 12/02/2018.
 */

public class TokenDescriptionHolder extends BinderViewHolder<Token> {

    public static final int VIEW_TYPE = 1067;

    private final TextView count;
    private final TextView title;

    public TokenDescriptionHolder(int resId, ViewGroup parent) {
        super(resId, parent);
        title = findViewById(R.id.name);
        count = findViewById(R.id.amount);
    }

    @Override
    public void bind(@Nullable Token token, @NonNull Bundle addition) {
        count.setText(String.valueOf(token.getTicketCount()));
        title.setText(token.tokenInfo.name);
    }
}
