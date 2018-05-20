package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.router.MarketBuyRouter;
import io.stormbird.wallet.service.MarketQueueService;

/**
 * Created by James on 20/02/2018.
 */

public class BrowseMarketViewModelFactory implements ViewModelProvider.Factory
{
    private final MarketQueueService marketQueueService;
    private final MarketBuyRouter marketBuyRouter;
    private final FetchTokensInteract fetchTokensInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;

    public BrowseMarketViewModelFactory(
            MarketQueueService marketQueueService,
            MarketBuyRouter marketBuyRouter,
            FetchTokensInteract fetchTokensInteract,
            FindDefaultWalletInteract findDefaultWalletInteract) {
        this.marketQueueService = marketQueueService;
        this.marketBuyRouter = marketBuyRouter;
        this.fetchTokensInteract = fetchTokensInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new BrowseMarketViewModel(marketQueueService, marketBuyRouter, fetchTokensInteract, findDefaultWalletInteract);
    }
}
