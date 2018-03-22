package io.awallet.crypto.alphawallet.ui.widget;

import android.view.View;

import io.awallet.crypto.alphawallet.entity.MarketplaceEvent;
import io.awallet.crypto.alphawallet.entity.Token;

public interface OnMarketplaceEventClickListener {
    void onMarketplaceEventClick(View view, MarketplaceEvent marketplaceEvent);
}
