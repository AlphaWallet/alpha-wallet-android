package com.wallet.crypto.alphawallet.viewmodel;

import android.content.Context;

import com.wallet.crypto.alphawallet.entity.MarketplaceEvent;
import com.wallet.crypto.alphawallet.router.MarketBrowseRouter;

public class MarketplaceViewModel extends BaseViewModel {
    private final MarketBrowseRouter marketBrowseRouter;

    MarketplaceViewModel(
            MarketBrowseRouter marketBrowseRouter
    ) {
        this.marketBrowseRouter = marketBrowseRouter;
    }

    public void showMarketplace(Context context, MarketplaceEvent marketplaceEvent) {
        marketBrowseRouter.open(context, marketplaceEvent);
    }
}
