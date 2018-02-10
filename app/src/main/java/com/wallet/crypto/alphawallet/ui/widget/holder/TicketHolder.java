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
import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.entity.Token;
import com.wallet.crypto.alphawallet.entity.TokenTicker;
import com.wallet.crypto.alphawallet.ui.widget.OnTokenClickListener;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Created by James on 9/02/2018.
 */

public class TicketHolder extends TokenHolder implements View.OnClickListener {

    public TicketHolder(int resId, ViewGroup parent) {
        super(resId, parent);
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
}
