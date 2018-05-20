package io.stormbird.wallet.viewmodel;

import android.content.Context;

import io.stormbird.wallet.entity.MarketplaceEvent;
import io.stormbird.wallet.router.MarketBrowseRouter;

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
