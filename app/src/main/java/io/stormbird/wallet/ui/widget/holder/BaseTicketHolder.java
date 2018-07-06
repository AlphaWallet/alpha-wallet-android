package io.stormbird.wallet.ui.widget.holder;

import java.text.ParseException;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.math.BigInteger;
import java.util.Locale;

import io.stormbird.token.util.ZonedDateTime;
import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.entity.Token;

import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.ui.widget.OnTicketIdClickListener;
import io.stormbird.token.entity.NonFungibleToken;
import io.stormbird.token.entity.TicketRange;

public class BaseTicketHolder extends BinderViewHolder<TicketRange> implements View.OnClickListener
{
    private TicketRange thisData;
    private Ticket ticket;
    private OnTicketIdClickListener onTicketClickListener;
    private final AssetDefinitionService assetService; //need to cache this locally, unless we cache every string we need in the constructor

    private final View activityView;
    protected final TextView ticketRedeemed;
    protected final LinearLayout ticketDetailsLayout;
    protected final LinearLayout ticketLayout;

    public BaseTicketHolder(int resId, ViewGroup parent, Token ticket, AssetDefinitionService service) {
        super(resId, parent);

        activityView = this.itemView;

        itemView.setOnClickListener(this);
        ticketRedeemed = findViewById(R.id.redeemed);
        ticketDetailsLayout = findViewById(R.id.layout_ticket_details);
        ticketLayout = findViewById(R.id.layout_select_ticket);
        assetService = service;
        this.ticket = (Ticket)ticket;
    }

    @Override
    public void bind(@Nullable TicketRange data, @NonNull Bundle addition)
    {
        this.thisData = data;

        if (data.tokenIds.size() > 0)
        {
            ticket.displayTicketHolder(data, activityView, assetService, getContext());
        }
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
