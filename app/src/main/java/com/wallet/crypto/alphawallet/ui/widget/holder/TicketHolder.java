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
import com.wallet.crypto.alphawallet.entity.TicketDecode;
import com.wallet.crypto.alphawallet.entity.Token;
import com.wallet.crypto.alphawallet.entity.TokenTicker;
import com.wallet.crypto.alphawallet.ui.widget.OnTicketIdClickListener;
import com.wallet.crypto.alphawallet.ui.widget.OnTokenClickListener;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Created by James on 9/02/2018.
 */

public class TicketHolder extends BinderViewHolder<Integer> implements View.OnClickListener {

    public static final int VIEW_TYPE = 1066;

    private Integer thisData;
    private OnTicketIdClickListener onTicketClickListener;

    public final TextView name;
    public final TextView amount;
    public final TextView venue;
    public final TextView date;
    public final TextView ticketIds;
    public final TextView ticketCat;

    public TicketHolder(int resId, ViewGroup parent) {
        super(resId, parent);
        name = findViewById(R.id.name);
        amount = findViewById(R.id.amount);
        venue = findViewById(R.id.venue);
        date = findViewById(R.id.date);
        ticketIds = findViewById(R.id.tickettext);
        ticketCat = findViewById(R.id.cattext);
        itemView.setOnClickListener(this);
    }

    @Override
    public void bind(@Nullable Integer data, @NonNull Bundle addition) {
        this.thisData = data;
        try {
            name.setText(TicketDecode.getName());
            amount.setText("x1");
            venue.setText(TicketDecode.getVenue(data));
            date.setText(TicketDecode.getDate(data));
            ticketIds.setText(TicketDecode.getSeatId(data));
            ticketCat.setText(TicketDecode.getZone(data));
        } catch (Exception ex) {
            fillEmpty();
        }
    }

    protected void fillEmpty() {
        name.setText(R.string.NA);
        venue.setText(R.string.NA);
    }

    @Override
    public void onClick(View v) {
        if (onTicketClickListener != null) {
            onTicketClickListener.onTicketIdClick(v, thisData);
        }
    }

    public void setOnTokenClickListener(OnTicketIdClickListener onTokenClickListener) {
        this.onTicketClickListener = onTokenClickListener;
    }
}
