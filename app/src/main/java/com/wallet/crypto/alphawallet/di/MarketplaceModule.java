package com.wallet.crypto.alphawallet.di;

import com.wallet.crypto.alphawallet.router.MarketBrowseRouter;
import com.wallet.crypto.alphawallet.viewmodel.MarketplaceViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class MarketplaceModule {
    @Provides
    MarketplaceViewModelFactory provideMarketplaceViewModelFactory(
            MarketBrowseRouter marketBrowseRouter) {
        return new MarketplaceViewModelFactory(marketBrowseRouter);
    }

    @Provides
    MarketBrowseRouter provideMarketBrowseRouter() { return new MarketBrowseRouter(); }
}
