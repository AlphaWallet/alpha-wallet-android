package io.stormbird.wallet.di;

import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.repository.TokenRepositoryType;
import io.stormbird.wallet.repository.WalletRepositoryType;
import io.stormbird.wallet.router.MarketBuyRouter;
import io.stormbird.wallet.service.MarketQueueService;
import io.stormbird.wallet.viewmodel.BrowseMarketViewModelFactory;

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
            MarketBuyRouter marketBuyRouter,
            FetchTokensInteract fetchTokensInteract,
            FindDefaultWalletInteract findDefaultWalletInteract) {
        return new BrowseMarketViewModelFactory(
                marketQueueService, marketBuyRouter, fetchTokensInteract, findDefaultWalletInteract);
    }

    @Provides
    MarketBuyRouter provideMarketBuyRouter() {
        return new MarketBuyRouter();
    }

    @Provides
    FetchTokensInteract provideFetchTokensInteract(TokenRepositoryType tokenRepository) {
        return new FetchTokensInteract(tokenRepository);
    }

    @Provides
    FindDefaultWalletInteract provideFindDefaultWalletInteract(WalletRepositoryType walletRepository) {
        return new FindDefaultWalletInteract(walletRepository);
    }
}