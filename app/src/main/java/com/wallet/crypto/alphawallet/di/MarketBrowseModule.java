package com.wallet.crypto.alphawallet.di;

import com.wallet.crypto.alphawallet.interact.FetchTokensInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import com.wallet.crypto.alphawallet.repository.TokenRepositoryType;
import com.wallet.crypto.alphawallet.repository.WalletRepositoryType;
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