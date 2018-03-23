package io.awallet.crypto.alphawallet.ui.widget.holder;

import android.animation.LayoutTransition;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.entity.TicketDecode;
import io.awallet.crypto.alphawallet.ui.widget.OnTicketIdClickListener;
import io.awallet.crypto.alphawallet.ui.widget.entity.TicketRange;

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
    private final TextView ticketRedeemed;
    private final LinearLayout ticketDetailsLayout;
    private final LinearLayout ticketLayout;

    public TicketHolder(int resId, ViewGroup parent) {
        super(resId, parent);
        name = findViewById(R.id.name);
        amount = findViewById(R.id.amount);
        venue = findViewById(R.id.venue);
        date = findViewById(R.id.date);
        ticketIds = findViewById(R.id.tickettext);
        ticketCat = findViewById(R.id.cattext);
        itemView.setOnClickListener(this);
        ticketRedeemed = findViewById(R.id.redeemed);
        ticketDetailsLayout = findViewById(R.id.layout_ticket_details);
        ticketLayout = findViewById(R.id.layout_select);
    }

    @Override
    public void bind(@Nullable TicketRange data, @NonNull Bundle addition) {
        this.thisData = data;
        try {
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

                if (data.isBurned)
                {
                    ticketRedeemed.setVisibility(View.VISIBLE);
                }
                else
                {
                    ticketRedeemed.setVisibility(View.GONE);
                }

//                ticketLayout.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
                ticketLayout.setOnClickListener(v -> {
                    if (ticketDetailsLayout.getVisibility() == View.VISIBLE) {
                        ticketDetailsLayout.setVisibility(View.GONE);
                    } else {
                        ticketDetailsLayout.setVisibility(View.VISIBLE);
                    }
                });
            }
            else
            {
                fillEmpty();
            }
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
