package io.awallet.crypto.alphawallet.di;


import io.awallet.crypto.alphawallet.interact.CreateTransactionInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import io.awallet.crypto.alphawallet.repository.EthereumNetworkRepositoryType;
import io.awallet.crypto.alphawallet.repository.PasswordStore;
import io.awallet.crypto.alphawallet.repository.TransactionRepositoryType;
import io.awallet.crypto.alphawallet.repository.WalletRepositoryType;
import io.awallet.crypto.alphawallet.service.MarketQueueService;
import io.awallet.crypto.alphawallet.viewmodel.PurchaseTicketsViewModelFactory;

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
            CreateTransactionInteract createTransactionInteract,
            MarketQueueService marketQueueService) {
        return new PurchaseTicketsViewModelFactory(
                findDefaultNetworkInteract, findDefaultWalletInteract, createTransactionInteract, marketQueueService);
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

    @Provides
    CreateTransactionInteract provideCreateTransactionInteract(TransactionRepositoryType transactionRepository, PasswordStore passwordStore) {
        return new CreateTransactionInteract(transactionRepository, passwordStore);
    }
}
