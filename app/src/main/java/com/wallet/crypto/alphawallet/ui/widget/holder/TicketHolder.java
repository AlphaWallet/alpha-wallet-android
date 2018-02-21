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
import com.wallet.crypto.alphawallet.ui.widget.entity.TicketRange;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Created by James on 9/02/2018.
 */

public class TicketHolder extends BinderViewHolder<TicketRange> implements View.OnClickListener {

    public static final int VIEW_TYPE = 1066;

    private TicketRange thisData;
    private OnTicketIdClickListener onTicketClickListener;

    private final TextView name;
    private final TextView amount;
    private final TextView date;
    private final TextView venue;
    private final TextView ticketIds;
    private final TextView ticketCat;

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
    public void bind(@Nullable TicketRange data, @NonNull Bundle addition) {
        this.thisData = data;
        try {
            String seatRange = String.valueOf(data.seatStart);
            if (data.seatCount > 1) seatRange = data.seatStart + "-" + (data.seatStart+(data.seatCount-1));
            String seatCount = String.format(Locale.getDefault(),"x%d", data.seatCount);
            name.setText(TicketDecode.getName());
            amount.setText(seatCount);
            venue.setText(TicketDecode.getVenue(data.tokenId));
            date.setText(TicketDecode.getDate(data.tokenId));
            ticketIds.setText(seatRange);
            ticketCat.setText(TicketDecode.getZone(data.tokenId));
        } catch (Exception ex) {
            fillEmpty();
        }
    }

    private void fillEmpty() {
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
