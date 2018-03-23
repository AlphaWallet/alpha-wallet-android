package io.awallet.crypto.alphawallet.di;

import io.awallet.crypto.alphawallet.interact.FetchTokensInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import io.awallet.crypto.alphawallet.repository.TokenRepositoryType;
import io.awallet.crypto.alphawallet.repository.WalletRepositoryType;
import io.awallet.crypto.alphawallet.router.MarketBuyRouter;
import io.awallet.crypto.alphawallet.service.MarketQueueService;
import io.awallet.crypto.alphawallet.viewmodel.BrowseMarketViewModelFactory;

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