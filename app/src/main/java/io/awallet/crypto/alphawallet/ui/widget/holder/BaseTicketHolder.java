package io.awallet.crypto.alphawallet.ui.widget.holder;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.math.BigInteger;
import java.util.Locale;

import io.awallet.crypto.alphawallet.R;
import io.awallet.crypto.alphawallet.entity.Token;
import io.awallet.crypto.alphawallet.repository.AssetDefinition;
import io.awallet.crypto.alphawallet.repository.entity.NonFungibleToken;
import io.awallet.crypto.alphawallet.ui.widget.OnTicketIdClickListener;
import io.awallet.crypto.alphawallet.ui.widget.entity.TicketRange;

public class BaseTicketHolder extends BinderViewHolder<TicketRange> implements View.OnClickListener
{
    private TicketRange thisData;
    private Token ticket;
    private OnTicketIdClickListener onTicketClickListener;
    private final AssetDefinition assetDefinition; //need to cache this locally, unless we cache every string we need in the constructor

    private final TextView name;
    private final TextView amount;
    private final TextView date;
    private final TextView venue;
    private final TextView ticketIds;
    private final TextView ticketCat;
    protected final TextView ticketRedeemed;
    private final TextView ticketDetails;
    protected final LinearLayout ticketDetailsLayout;
    protected final LinearLayout ticketLayout;

    public BaseTicketHolder(int resId, ViewGroup parent, AssetDefinition definition, Token ticket) {
        super(resId, parent);
        name = findViewById(R.id.name);
        amount = findViewById(R.id.amount);
        venue = findViewById(R.id.venue);
        date = findViewById(R.id.date);
        ticketIds = findViewById(R.id.tickettext);
        ticketCat = findViewById(R.id.cattext);
        ticketDetails = findViewById(R.id.ticket_details);
        itemView.setOnClickListener(this);
        ticketRedeemed = findViewById(R.id.redeemed);
        ticketDetailsLayout = findViewById(R.id.layout_ticket_details);
        ticketLayout = findViewById(R.id.layout_select);
        assetDefinition = definition;
        this.ticket = ticket;
    }

    @Override
    public void bind(@Nullable TicketRange data, @NonNull Bundle addition)
    {
        this.thisData = data;
        name.setText(ticket.tokenInfo.name);

        try
        {
            if (data.tokenIds.size() > 0)
            {
                BigInteger firstTokenId = data.tokenIds.get(0);
                String seatCount = String.format(Locale.getDefault(), "x%d", data.tokenIds.size());
                NonFungibleToken nonFungibleToken = new NonFungibleToken(firstTokenId, assetDefinition);
                amount.setText(seatCount);
                venue.setText(nonFungibleToken.getAttribute("venue").text);
                date.setText(nonFungibleToken.getDate("dd MMM"));
                ticketIds.setText(nonFungibleToken.getRangeStr(data));
                ticketCat.setText(nonFungibleToken.getAttribute("category").text);
                ticketDetails.setText(ticket.getTicketInfo(nonFungibleToken));
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
