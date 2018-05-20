package io.stormbird.wallet.ui.widget;

import android.view.View;

import io.stormbird.wallet.entity.MarketplaceEvent;
import io.stormbird.wallet.entity.Token;

public interface OnMarketplaceEventClickListener {
    void onMarketplaceEventClick(View view, MarketplaceEvent marketplaceEvent);
}
