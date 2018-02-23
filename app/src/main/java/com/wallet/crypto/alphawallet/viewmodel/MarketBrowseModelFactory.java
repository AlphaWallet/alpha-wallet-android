package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.wallet.crypto.alphawallet.router.MarketBuyRouter;
import com.wallet.crypto.alphawallet.router.PurchaseTicketRouter;
import com.wallet.crypto.alphawallet.service.MarketQueueService;

/**
 * Created by James on 20/02/2018.
 */

public class MarketBrowseModelFactory implements ViewModelProvider.Factory
{
    private final MarketQueueService marketQueueService;
    private final MarketBuyRouter marketBuyRouter;
    private final PurchaseTicketRouter purchaseTicketRouter;

    public MarketBrowseModelFactory(
            MarketQueueService marketQueueService,
            MarketBuyRouter marketBuyRouter,
            PurchaseTicketRouter purchaseTicketRouter) {
        this.marketQueueService = marketQueueService;
        this.marketBuyRouter = marketBuyRouter;
        this.purchaseTicketRouter = purchaseTicketRouter;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new MarketBrowseModel(marketQueueService, marketBuyRouter, purchaseTicketRouter);
    }
}
