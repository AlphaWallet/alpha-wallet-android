package io.stormbird.wallet.ui.widget;

import android.view.View;

import io.stormbird.wallet.entity.MarketplaceEvent;

public interface OnMarketplaceEventClickListener {
    void onMarketplaceEventClick(View view, MarketplaceEvent marketplaceEvent);
}
