package com.wallet.crypto.trustapp.ui.widget.holder;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.entity.Token;
import com.wallet.crypto.trustapp.ui.widget.OnTokenClickListener;

import java.math.BigDecimal;

public class TokenHolder extends BinderViewHolder<Token> implements View.OnClickListener {

    public final TextView symbol;
    public final TextView balance;
    public final ImageView icon;
    public final TextView arrayBalance;

    public Token token;
    private OnTokenClickListener onTokenClickListener;

    public TokenHolder(int resId, ViewGroup parent) {
        super(resId, parent);

        symbol = findViewById(R.id.symbol);
        balance = findViewById(R.id.balance);
        icon = findViewById(R.id.logo);
        arrayBalance = findViewById(R.id.balanceArray);
        itemView.setOnClickListener(this);
    }

    @Override
    public void bind(@Nullable Token data, @NonNull Bundle addition) {
        this.token = data;
        if (data == null) {
            fillEmpty();
            return;
        }
        try {
            //customise the token display according to token contents
            data.tokenInfo.setupContent(this);
        } catch (Exception e) {
            fillEmpty();
        }
    }

    private void fillEmpty() {
        balance.setText(R.string.NA);
        balance.setText(R.string.minus);
    }

    @Override
    public void onClick(View v) {
        if (onTokenClickListener != null) {
            onTokenClickListener.onTokenClick(v, token);
        }
    }

    public void setOnTokenClickListener(OnTokenClickListener onTokenClickListener) {
        this.onTokenClickListener = onTokenClickListener;
    }
}
