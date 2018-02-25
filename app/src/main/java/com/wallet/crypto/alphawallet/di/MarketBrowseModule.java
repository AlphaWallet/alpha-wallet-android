package com.wallet.crypto.alphawallet.di;

import com.wallet.crypto.alphawallet.interact.FetchTokensInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import com.wallet.crypto.alphawallet.repository.EthereumNetworkRepositoryType;
import com.wallet.crypto.alphawallet.repository.TokenRepositoryType;
import com.wallet.crypto.alphawallet.repository.WalletRepositoryType;
import com.wallet.crypto.alphawallet.router.ConfirmationRouter;
import com.wallet.crypto.alphawallet.router.MarketBuyRouter;
import com.wallet.crypto.alphawallet.service.MarketQueueService;
import com.wallet.crypto.alphawallet.viewmodel.MarketBrowseModelFactory;
import com.wallet.crypto.alphawallet.viewmodel.SellTicketModelFactory;

import dagger.Module;
import dagger.Provides;

/**
 * Created by James on 20/02/2018.
 */

@Module
public class MarketBrowseModule
{
    @Provides
    MarketBrowseModelFactory marketBrowseModelFactory(
            MarketQueueService marketQueueService,
            MarketBuyRouter marketBuyRouter) {
        return new MarketBrowseModelFactory(
                marketQueueService, marketBuyRouter);
    }

    @Provides
    MarketBuyRouter provideMarketBuyRouter() {
        return new MarketBuyRouter();
    }
}