package com.wallet.crypto.alphawallet.ui.widget.holder;

/**
 * Created by James on 27/02/2018.
 */

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

public class RedeemTicketHolder extends BinderViewHolder<Token> {

    public static final int VIEW_TYPE = 1299;

    private final TextView count;
    private final TextView title;
    private final TextView ticketType;
    private final TextView data;

    public RedeemTicketHolder(int resId, ViewGroup parent) {
        super(resId, parent);
        title = findViewById(R.id.name);
        count = findViewById(R.id.amount);
        ticketType = findViewById(R.id.textView2);
        data = findViewById(R.id.textView2);
    }

    @Override
    public void bind(@Nullable Token token, @NonNull Bundle addition)
    {
        count.setVisibility(View.GONE);
        title.setText(R.string.select_tickets_redeem);
        ticketType.setText(token.tokenInfo.name);
        data.setVisibility(View.GONE);
    }
}
