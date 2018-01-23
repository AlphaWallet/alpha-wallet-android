package com.wallet.crypto.trustapp.ui.widget.holder;

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
import android.widget.TextView;

import com.wallet.crypto.trustapp.R;
import com.wallet.crypto.trustapp.entity.Token;
import com.wallet.crypto.trustapp.ui.widget.OnTokenClickListener;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TokenHolder extends BinderViewHolder<Token> implements View.OnClickListener {

    public static final int VIEW_TYPE = 1005;
    private final TextView symbol;
    private final TextView balanceEth;
    private final TextView balanceCurrency;

    private Token token;
    private OnTokenClickListener onTokenClickListener;

    public TokenHolder(int resId, ViewGroup parent) {
        super(resId, parent);

        symbol = findViewById(R.id.symbol);
        balanceEth = findViewById(R.id.balance_eth);
        balanceCurrency = findViewById(R.id.balance_currency);
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
            if (TextUtils.isEmpty(token.tokenInfo.name)) {
                symbol.setText(token.tokenInfo.symbol);
            } else {
                symbol.setText(token.tokenInfo.name + " (" + token.tokenInfo.symbol + ")");
            }

            BigDecimal decimalDivisor = new BigDecimal(Math.pow(10, token.tokenInfo.decimals));
            BigDecimal ethBalance = token.tokenInfo.decimals > 0
                    ? token.balance.divide(decimalDivisor) : token.balance;
            String value = ethBalance.compareTo(BigDecimal.ZERO) == 0
                    ? "0"
                    : ethBalance.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
            this.balanceEth.setText(value);
            if (data.ticker == null) {
                this.balanceCurrency.setVisibility(View.GONE);
            } else {
                String converted = ethBalance.compareTo(BigDecimal.ZERO) == 0
                        ? "\u2014"
                        : ethBalance.multiply(new BigDecimal(data.ticker.price))
                            .setScale(2, RoundingMode.HALF_UP)
                            .stripTrailingZeros()
                            .toPlainString();
                this.balanceCurrency.setVisibility(View.VISIBLE);
                String formattedPercents = "";
                int color = Color.RED;
                try {
                    double percentage = Double.valueOf(data.ticker.percentChange24h);
                    color = ContextCompat.getColor(getContext(), percentage < 0 ? R.color.red : R.color.green);
                    formattedPercents = "(" + (percentage < 0 ? "-" : "+") + data.ticker.percentChange24h + "%)";
                } catch (Exception ex) { /* Quietly */ }
                String lbl = getString(R.string.token_balance,
                        ethBalance.compareTo(BigDecimal.ZERO) == 0 ? "" : "$",
                        converted, formattedPercents);
                Spannable spannable = new SpannableString(lbl);
                spannable.setSpan(new ForegroundColorSpan(color),
                        converted.length() + 1, lbl.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                this.balanceCurrency.setText(spannable);
            }
        } catch (Exception e) {
            fillEmpty();
        }
    }

    private void fillEmpty() {
        balanceEth.setText(R.string.NA);
        balanceCurrency.setVisibility(View.GONE);
//        balance.setText(R.string.minus);
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
