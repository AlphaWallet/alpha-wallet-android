package io.awallet.crypto.alphawallet.ui.widget.holder;

import android.animation.LayoutTransition;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.entity.TicketDecode;
import io.awallet.crypto.alphawallet.entity.Token;
import io.awallet.crypto.alphawallet.repository.AssetDefinition;
import io.awallet.crypto.alphawallet.repository.entity.NonFungibleToken;
import io.awallet.crypto.alphawallet.ui.widget.OnTicketIdClickListener;
import io.awallet.crypto.alphawallet.ui.widget.entity.TicketRange;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by James on 9/02/2018.
 */

public class TicketHolder extends BinderViewHolder<TicketRange> implements View.OnClickListener {

    public static final int VIEW_TYPE = 1066;

    private TicketRange thisData;
    private Token ticket;
    private OnTicketIdClickListener onTicketClickListener;
    private AssetDefinition assetDefinition; //need to cache this locally, unless we cache every string we need in the constructor

    private final TextView name;
    private final TextView amount;
    private final TextView date;
    private final TextView venue;
    private final TextView ticketIds;
    private final TextView ticketCat;
    private final TextView ticketRedeemed;
    private final LinearLayout ticketDetailsLayout;
    private final LinearLayout ticketLayout;

    public TicketHolder(int resId, ViewGroup parent, AssetDefinition definition, Token ticket) {
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
        assetDefinition = definition;
        this.ticket = ticket;
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
                //first retrieve name from ticket
                name.setText(ticket.tokenInfo.name);

                //Test data.
                //03 04 5AF6D740 474252 415247 01 01 04d2
                //03045AF6D740474252415247010104d2
                BigInteger bi = new BigInteger("03045AF6D740474252415247010104d2", 16);

                NonFungibleToken nonFungibleToken = new NonFungibleToken(bi, assetDefinition);

                amount.setText(seatCount);

                venue.setText(nonFungibleToken.getAttribute("venue").text);
                long dateUTC = nonFungibleToken.getAttribute("time").value.longValue();
                Date dateFormat = new java.util.Date(dateUTC*1000L);
                SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
                date.setText(dateFormatter.format(dateFormat.getTime()));
                ticketIds.setText(seatRange);
                ticketCat.setText(nonFungibleToken.getAttribute("category").text);

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
