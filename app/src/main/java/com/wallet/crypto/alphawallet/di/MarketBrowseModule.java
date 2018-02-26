package com.wallet.crypto.alphawallet.di;

import com.wallet.crypto.alphawallet.router.MarketBuyRouter;
import com.wallet.crypto.alphawallet.service.MarketQueueService;
import com.wallet.crypto.alphawallet.viewmodel.BrowseMarketViewModelFactory;

import dagger.Module;
import dagger.Provides;

/**
 * Created by James on 20/02/2018.
 */

@Module
public class MarketBrowseModule
{
    @Provides
    BrowseMarketViewModelFactory marketBrowseModelFactory(
            MarketQueueService marketQueueService,
            MarketBuyRouter marketBuyRouter) {
        return new BrowseMarketViewModelFactory(
                marketQueueService, marketBuyRouter);
    }

    @Provides
    MarketBuyRouter provideMarketBuyRouter() {
        return new MarketBuyRouter();
    }
}