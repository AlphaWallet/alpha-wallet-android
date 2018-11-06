package io.stormbird.wallet.di;


import io.stormbird.wallet.interact.CreateTransactionInteract;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.repository.PasswordStore;
import io.stormbird.wallet.repository.TransactionRepositoryType;
import io.stormbird.wallet.repository.WalletRepositoryType;
import io.stormbird.wallet.service.MarketQueueService;
import io.stormbird.wallet.service.TokensService;
import io.stormbird.wallet.viewmodel.PurchaseTicketsViewModelFactory;

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
            MarketQueueService marketQueueService,
            TokensService tokensService) {
        return new PurchaseTicketsViewModelFactory(
                findDefaultNetworkInteract, findDefaultWalletInteract, createTransactionInteract, marketQueueService, tokensService);
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
