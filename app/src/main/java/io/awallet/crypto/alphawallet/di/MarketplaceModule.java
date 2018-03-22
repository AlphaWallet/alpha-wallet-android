package io.awallet.crypto.alphawallet.di;

import io.awallet.crypto.alphawallet.router.MarketBrowseRouter;
import io.awallet.crypto.alphawallet.viewmodel.MarketplaceViewModelFactory;

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
