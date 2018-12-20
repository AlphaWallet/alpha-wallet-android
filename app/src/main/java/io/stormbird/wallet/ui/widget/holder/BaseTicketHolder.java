package io.stormbird.wallet.ui.widget.holder;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.entity.Token;

import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.token.entity.TicketRange;
import io.stormbird.wallet.ui.widget.OnTokenClickListener;

public class BaseTicketHolder extends BinderViewHolder<TicketRange> implements View.OnClickListener
{
    private TicketRange thisData;
    private Ticket ticket;
    private OnTokenClickListener onTokenClickListener;
    private final AssetDefinitionService assetService; //need to cache this locally, unless we cache every string we need in the constructor

    private final View activityView;
    protected final TextView ticketRedeemed;
    protected final LinearLayout ticketDetailsLayout;
    protected final LinearLayout ticketLayout;
    protected final TextView ticketDetails;

    public BaseTicketHolder(int resId, ViewGroup parent, Token ticket, AssetDefinitionService service) {
        super(resId, parent);

        activityView = this.itemView;

        itemView.setOnClickListener(this);
        ticketRedeemed = findViewById(R.id.redeemed);
        ticketDetailsLayout = findViewById(R.id.layout_ticket_details);
        ticketDetails = findViewById(R.id.ticket_details);
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
        if (onTokenClickListener != null) {
            onTokenClickListener.onTokenClick(v, ticket, thisData.tokenIds.get(0));
        }
    }

    public void setOnTokenClickListener(OnTokenClickListener onTokenClickListener) {
        this.onTokenClickListener = onTokenClickListener;
    }
}
