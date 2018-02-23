package com.wallet.crypto.alphawallet.di;


import com.wallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import com.wallet.crypto.alphawallet.repository.EthereumNetworkRepositoryType;
import com.wallet.crypto.alphawallet.repository.WalletRepositoryType;
import com.wallet.crypto.alphawallet.service.MarketQueueService;
import com.wallet.crypto.alphawallet.viewmodel.PurchaseTicketsViewModelFactory;

import dagger.Module;
import dagger.Provides;

/**
 * Created by James on 23/02/2018.
 */

@Module
public class PurchaseTicketsModule
{
    @Provides
    PurchaseTicketsViewModelFactory purchaseTicketsViewModelFactory(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            MarketQueueService marketQueueService) {
        return new PurchaseTicketsViewModelFactory(
                findDefaultNetworkInteract, findDefaultWalletInteract, marketQueueService);
    }

    @Provides
    FindDefaultNetworkInteract provideFindDefaultNetworkInteract(
            EthereumNetworkRepositoryType networkRepository) {
        return new FindDefaultNetworkInteract(networkRepository);
    }

    @Provides
    FindDefaultWalletInteract provideFindDefaultWalletInteract(WalletRepositoryType walletRepository) {
        return new FindDefaultWalletInteract(walletRepository);
    }
}
