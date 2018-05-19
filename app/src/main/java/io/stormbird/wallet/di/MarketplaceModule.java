package io.stormbird.wallet.di;

import io.stormbird.wallet.router.MarketBrowseRouter;
import io.stormbird.wallet.viewmodel.MarketplaceViewModelFactory;

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
