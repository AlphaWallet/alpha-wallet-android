package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.wallet.crypto.alphawallet.router.MarketBuyRouter;
import com.wallet.crypto.alphawallet.service.MarketQueueService;

/**
 * Created by James on 20/02/2018.
 */

public class BrowseMarketViewModelFactory implements ViewModelProvider.Factory
{
    private final MarketQueueService marketQueueService;
    private final MarketBuyRouter marketBuyRouter;

    public BrowseMarketViewModelFactory(
            MarketQueueService marketQueueService,
            MarketBuyRouter marketBuyRouter) {
        this.marketQueueService = marketQueueService;
        this.marketBuyRouter = marketBuyRouter;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new BrowseMarketViewModel(marketQueueService, marketBuyRouter);
    }
}
