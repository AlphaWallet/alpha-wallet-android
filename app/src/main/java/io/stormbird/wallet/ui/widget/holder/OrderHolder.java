package io.stormbird.wallet.ui.widget.holder;

import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import io.stormbird.wallet.R;
import io.stormbird.wallet.ui.widget.OnSalesOrderClickListener;
import io.stormbird.token.entity.MagicLinkData;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by James on 21/02/2018.
 */

public class OrderHolder extends BinderViewHolder<MagicLinkData> implements View.OnClickListener {

    public static final int VIEW_TYPE = 1166;

    private MagicLinkData thisData;
    private OnSalesOrderClickListener onOrderClickListener;

    private final TextView price;
    private final TextView count;
    private final TextView date;
    private final TextView name;
    private final TextView ticketIds;
    private final TextView ticketCat;
    private final TextView ticketTypeText;
    private final RelativeLayout updateOverlay;
    private final RelativeLayout unavailableOverlay;
    private final LinearLayout ticketLayout;
    private ImageView calendarImg;
    private ImageView ticketImg;
    private ImageView catImg;

    public OrderHolder(int resId, ViewGroup parent) {
        super(resId, parent);
        name = findViewById(R.id.name);
        price = findViewById(R.id.price);
        count = findViewById(R.id.ticket_count);
        date = findViewById(R.id.date);
        ticketIds = findViewById(R.id.tickettext);
        ticketCat = findViewById(R.id.cattext);
        ticketTypeText = findViewById(R.id.ticket_type);
        updateOverlay = findViewById(R.id.update_overlay);
        unavailableOverlay = findViewById(R.id.unavailable_overlay);
        ticketLayout = findViewById(R.id.layout_select_ticket);
        calendarImg = findViewById(R.id.calendar);
        ticketImg = findViewById(R.id.ticketicon);
        catImg = findViewById(R.id.caticon);
        itemView.setOnClickListener(this);
    }

    @Override
    public void bind(@Nullable MagicLinkData data, @NonNull Bundle addition) {
        // wouldn't it be a good idea to make MagicLinkData and TicketRange inherit from the same class?
        // therefore this bind and BaseTicketHolder.bind can merge. - Weiwu
        this.thisData = data;
        try {
            //TODO:`use XML data
//            int seatStart = TicketDecode.getSeatIdInt(data.ticketStart);
//            String seatRange = String.valueOf(seatStart);
//            if (data.ticketCount > 1) seatRange = seatStart + "-" + (seatStart+(data.ticketCount-1));
//            price.setText(String.valueOf(data.price));
            name.setText("XML Decoder, OrderHolder.java");
//            setBalance(data);
//            date.setText(TicketDecode.getDate(data.ticketStart));
//            ticketIds.setText(seatRange);
//            name.setText(TicketDecode.getZone(data.ticketStart) + data.contractName);
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

    public void setOnOrderClickListener(OnSalesOrderClickListener onTokenClickListener) {
        this.onOrderClickListener = onTokenClickListener;
    }
}
