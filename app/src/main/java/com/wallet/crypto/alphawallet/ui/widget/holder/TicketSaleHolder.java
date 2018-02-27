package com.wallet.crypto.alphawallet.ui.widget.holder;


import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatRadioButton;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.entity.TicketDecode;
import com.wallet.crypto.alphawallet.entity.Token;
import com.wallet.crypto.alphawallet.entity.TokenTicker;
import com.wallet.crypto.alphawallet.ui.widget.OnTicketIdClickListener;
import com.wallet.crypto.alphawallet.ui.widget.OnTokenCheckListener;
import com.wallet.crypto.alphawallet.ui.widget.OnTokenClickListener;
import com.wallet.crypto.alphawallet.ui.widget.entity.TicketRange;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Created by James on 12/02/2018.
 */

public class TicketSaleHolder extends BinderViewHolder<TicketRange> implements View.OnClickListener
{
    public static final int VIEW_TYPE = 1071;

    private TicketRange thisData;
    private OnTicketIdClickListener onTicketClickListener;
    private OnTokenCheckListener onTokenCheckListener;

    public final AppCompatRadioButton select;
    public final TextView name;
    public final TextView amount;
    public final TextView venue;
    public final TextView date;
    public final TextView ticketIds;
    public final TextView ticketCat;

    public TicketSaleHolder(int resId, ViewGroup parent)
    {
        super(resId, parent);
        name = findViewById(R.id.name);
        amount = findViewById(R.id.amount);
        venue = findViewById(R.id.venue);
        date = findViewById(R.id.date);
        ticketIds = findViewById(R.id.tickettext);
        ticketCat = findViewById(R.id.cattext);
        select = findViewById(R.id.radioBox);
        itemView.setOnClickListener(this);
    }

    @Override
    public void bind(@Nullable TicketRange data, @NonNull Bundle addition)
    {
        this.thisData = data;
        try
        {
            if (data.tokenIds.size() > 0)
            {
                int firstTokenId = data.tokenIds.get(0);
                int seatStart = TicketDecode.getSeatIdInt(firstTokenId);
                String seatRange = String.valueOf(seatStart);
                if (data.tokenIds.size() > 1)
                    seatRange = seatStart + "-" + (seatStart + (data.tokenIds.size() - 1));
                String seatCount = String.format(Locale.getDefault(), "x%d", data.tokenIds.size());
                name.setText(TicketDecode.getName());
                amount.setText(seatCount);
                venue.setText(TicketDecode.getVenue(firstTokenId));
                date.setText(TicketDecode.getDate(firstTokenId));
                ticketIds.setText(seatRange);
                ticketCat.setText(TicketDecode.getZone(firstTokenId));
                select.setVisibility(View.VISIBLE);

                select.setOnCheckedChangeListener(null); //have to invalidate listener first otherwise we trigger cached listener and create infinite loop
                select.setChecked(thisData.isChecked);

                select.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b)
                    {
                        if (b) {
                            onTokenCheckListener.onTokenCheck(thisData);
                        }
                    }
                });
            }
            else
            {
                fillEmpty();
            }
        }
        catch (Exception ex)
        {
            fillEmpty();
        }
    }

    protected void fillEmpty()
    {
        name.setText(R.string.NA);
        venue.setText(R.string.NA);
    }

    @Override
    public void onClick(View v)
    {
        if (onTicketClickListener != null)
        {
            onTicketClickListener.onTicketIdClick(v, thisData);
        }
    }

    public void setOnTokenClickListener(OnTicketIdClickListener onTokenClickListener)
    {
        this.onTicketClickListener = onTokenClickListener;
    }

    public void setOnTokenCheckListener(OnTokenCheckListener onTokenCheckListener)
    {
        this.onTokenCheckListener = onTokenCheckListener;
    }
}
