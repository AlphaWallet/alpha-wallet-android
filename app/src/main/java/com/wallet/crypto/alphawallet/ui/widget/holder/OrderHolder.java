package com.wallet.crypto.alphawallet.ui.widget.holder;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wallet.crypto.alphawallet.R;
import com.wallet.crypto.alphawallet.entity.MarketInstance;
import com.wallet.crypto.alphawallet.entity.TicketDecode;
import com.wallet.crypto.alphawallet.ui.widget.OnMarketInstanceClickListener;
import com.wallet.crypto.alphawallet.ui.widget.OnTicketIdClickListener;
import com.wallet.crypto.alphawallet.ui.widget.entity.TicketRange;

import java.util.Locale;

/**
 * Created by James on 21/02/2018.
 */

public class OrderHolder extends BinderViewHolder<MarketInstance> implements View.OnClickListener {

    public static final int VIEW_TYPE = 1166;

    private MarketInstance thisData;
    private OnMarketInstanceClickListener onOrderClickListener;

    private final TextView price;
    private final TextView count;
    private final TextView date;
    private final TextView name;
    private final TextView ticketIds;
    private final TextView ticketCat;

    public OrderHolder(int resId, ViewGroup parent) {
        super(resId, parent);
        name = findViewById(R.id.name);
        price = findViewById(R.id.price);
        count = findViewById(R.id.ticket_count);
        date = findViewById(R.id.date);
        ticketIds = findViewById(R.id.tickettext);
        ticketCat = findViewById(R.id.cattext);
        itemView.setOnClickListener(this);
    }

    @Override
    public void bind(@Nullable MarketInstance data, @NonNull Bundle addition) {
        this.thisData = data;
        try {
            int seatStart = TicketDecode.getSeatIdInt(data.ticketStart);
            String seatRange = String.valueOf(seatStart);
            if (data.ticketCount > 1) seatRange = seatStart + "-" + (seatStart+(data.ticketCount-1));
            price.setText(String.valueOf(data.price));
            name.setText(TicketDecode.getName());
            count.setText(String.valueOf(data.ticketCount));
            date.setText(TicketDecode.getDate(data.ticketStart));
            ticketIds.setText(seatRange);
            ticketCat.setText(TicketDecode.getZone(data.ticketStart));
        } catch (Exception ex) {
            fillEmpty();
        }
    }

    private void fillEmpty() {
        name.setText(R.string.NA);
        price.setText(R.string.NA);
    }

    @Override
    public void onClick(View v) {
        if (onOrderClickListener != null) {
            onOrderClickListener.onOrderClick(v, thisData);
        }
    }

    public void setOnOrderClickListener(OnMarketInstanceClickListener onTokenClickListener) {
        this.onOrderClickListener = onTokenClickListener;
    }
}
