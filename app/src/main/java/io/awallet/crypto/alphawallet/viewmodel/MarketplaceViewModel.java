package io.awallet.crypto.alphawallet.viewmodel;

import android.content.Context;

import io.awallet.crypto.alphawallet.entity.MarketplaceEvent;
import io.awallet.crypto.alphawallet.router.MarketBrowseRouter;

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
