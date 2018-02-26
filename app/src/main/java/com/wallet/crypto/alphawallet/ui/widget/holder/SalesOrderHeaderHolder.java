package com.wallet.crypto.alphawallet.ui.widget.holder;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.Token;

/**
 * Created by James on 13/02/2018.
 */

public class SalesOrderHeaderHolder extends BinderViewHolder<Token> {

    public static final int VIEW_TYPE = 1068;

    private final TextView count;
    private final TextView title;
    private final TextView ticketType;

    public SalesOrderHeaderHolder(int resId, ViewGroup parent) {
        super(resId, parent);
        title = findViewById(R.id.name);
        count = findViewById(R.id.amount);
        ticketType = findViewById(R.id.textView2);
    }

    @Override
    public void bind(@Nullable Token token, @NonNull Bundle addition)
    {
        count.setVisibility(View.GONE);
        title.setText(R.string.select_tickets);
        ticketType.setText(token.tokenInfo.name);
    }
}
