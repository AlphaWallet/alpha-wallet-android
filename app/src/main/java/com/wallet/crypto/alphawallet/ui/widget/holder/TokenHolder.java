package com.wallet.crypto.alphawallet.ui.widget.holder;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.Token;
import com.wallet.crypto.alphawallet.entity.TokenTicker;
import com.wallet.crypto.alphawallet.ui.widget.OnTokenClickListener;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TokenHolder extends BinderViewHolder<Token> implements View.OnClickListener {

    public static final int VIEW_TYPE = 1005;
    public static final String EMPTY_BALANCE = "\u2014\u2014";

    public final TextView symbol;
    public final TextView balanceEth;
    public final TextView balanceCurrency;
    public final ImageView icon;
    public final TextView arrayBalance;

    public Token token;
    private OnTokenClickListener onTokenClickListener;

    public TokenHolder(int resId, ViewGroup parent) {
        super(resId, parent);

        icon = findViewById(R.id.icon);
        symbol = findViewById(R.id.symbol);
        balanceEth = findViewById(R.id.balance_eth);
        balanceCurrency = findViewById(R.id.balance_currency);
        arrayBalance = findViewById(R.id.balanceArray);
        itemView.setOnClickListener(this);
    }

    @Override
    public void bind(@Nullable Token data, @NonNull Bundle addition) {
        this.token = data;
        try {
            // We handled NPE. Exception handling is expensive, but not impotent here
            symbol.setText(TextUtils.isEmpty(token.tokenInfo.name)
                        ? token.tokenInfo.symbol
                        : getString(R.string.token_name, token.tokenInfo.name, token.tokenInfo.symbol));

            token.setupContent(this);
        } catch (Exception ex) {
            fillEmpty();
        }
    }

    public void fillIcon(String imageUrl, int defaultResId) {
        if (TextUtils.isEmpty(imageUrl)) {
            icon.setImageResource(defaultResId);
        } else {
            Picasso.with(getContext())
                    .load(imageUrl)
                    .fit()
                    .centerInside()
                    .placeholder(defaultResId)
                    .error(defaultResId)
                    .into(icon);
        }
    }

    public void fillCurrency(BigDecimal ethBalance, TokenTicker ticker) {
        String converted = ethBalance.compareTo(BigDecimal.ZERO) == 0
                ? EMPTY_BALANCE
                : ethBalance.multiply(new BigDecimal(ticker.price))
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
        String formattedPercents = "";
        int color = Color.RED;
        try {
            double percentage = Double.valueOf(ticker.percentChange24h);
            color = ContextCompat.getColor(getContext(), percentage < 0 ? R.color.red : R.color.green);
            formattedPercents = "(" + (percentage < 0 ? "" : "+") + ticker.percentChange24h + "%)";
        } catch (Exception ex) { /* Quietly */ }
        String lbl = getString(R.string.token_balance,
                ethBalance.compareTo(BigDecimal.ZERO) == 0 ? "" : "$",
                converted, formattedPercents);
        Spannable spannable = new SpannableString(lbl);
        spannable.setSpan(new ForegroundColorSpan(color),
                converted.length() + 1, lbl.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        this.balanceCurrency.setText(spannable);
    }

    protected void fillEmpty() {
        balanceEth.setText(R.string.NA);
        balanceCurrency.setText(EMPTY_BALANCE);
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
