package io.stormbird.wallet.ui.widget.holder;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.Ticket;
import io.stormbird.wallet.entity.Token;

import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.token.entity.TicketRange;
import io.stormbird.wallet.ui.widget.OnTokenClickListener;

import java.math.BigInteger;

public class BaseTicketHolder extends BinderViewHolder<TicketRange> implements View.OnClickListener, View.OnLongClickListener
{
    private TicketRange thisData;
    private Token token;
    private OnTokenClickListener onTokenClickListener;
    private final AssetDefinitionService assetService; //need to cache this locally, unless we cache every string we need in the constructor

    private final View activityView;
    protected final RelativeLayout ticketLayout;

    public BaseTicketHolder(int resId, ViewGroup parent, Token ticket, AssetDefinitionService service) {
        super(resId, parent);

        activityView = this.itemView;

        itemView.setOnClickListener(this);
        ticketLayout = findViewById(R.id.layout_select_ticket);
        assetService = service;
        token = ticket;
    }

    @Override
    public void bind(@Nullable TicketRange data, @NonNull Bundle addition)
    {
        this.thisData = data;

        if (data.tokenIds.size() > 0)
        {
            token.displayTicketHolder(data, activityView, assetService, getContext());
        }
    }

    @Override
    public void onClick(View v) {
        if (onTokenClickListener != null) {
            onTokenClickListener.onTokenClick(v, token, thisData.tokenIds, true);
        }
    }

    public void setOnTokenClickListener(OnTokenClickListener onTokenClickListener) {
        this.onTokenClickListener = onTokenClickListener;
    }

    @Override
    public boolean onLongClick(View view)
    {
        if (onTokenClickListener != null)
        {
            onTokenClickListener.onLongTokenClick(view, token, thisData.tokenIds);
        }

        return true;
    }
}
