package com.alphawallet.app.ui.widget.holder;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.alphawallet.app.R;

import com.alphawallet.app.entity.Token;

import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.token.entity.TicketRange;
import com.alphawallet.app.ui.widget.OnTokenClickListener;

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
