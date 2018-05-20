package io.stormbird.wallet.ui.widget.holder;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.stormbird.wallet.R;
import io.stormbird.wallet.entity.MarketplaceEvent;
import io.stormbird.wallet.ui.widget.OnMarketplaceEventClickListener;

public class MarketplaceEventHolder extends BinderViewHolder<MarketplaceEvent> implements View.OnClickListener{

    public static final int VIEW_TYPE = 1091;

    private final TextView eventName;

    private MarketplaceEvent marketplaceEvent;

    private OnMarketplaceEventClickListener onMarketplaceEventClickListener;

    public MarketplaceEventHolder(int resId, ViewGroup parent) {
        super(resId, parent);
        eventName = findViewById(R.id.event_name);
        itemView.setOnClickListener(this);
    }
    @Override
    public void bind(@Nullable MarketplaceEvent marketplaceEvent, @NonNull Bundle addition) {
        this.marketplaceEvent = marketplaceEvent;
        eventName.setText(marketplaceEvent.getEventName());
    }

    public void setOnMarketplaceEventClickListener(OnMarketplaceEventClickListener onMarketplaceEventClickListener) {
        this.onMarketplaceEventClickListener = onMarketplaceEventClickListener;
    }

    @Override
    public void onClick(View v) {
        if (onMarketplaceEventClickListener != null) {
            onMarketplaceEventClickListener.onMarketplaceEventClick(v, marketplaceEvent);
        }
    }
}
