package io.stormbird.wallet.di;

import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.GenericWalletInteract;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.repository.WalletRepositoryType;
import io.stormbird.wallet.router.ExternalBrowserRouter;
import io.stormbird.wallet.service.TokensService;
import io.stormbird.wallet.viewmodel.TransactionDetailViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
public class TransactionDetailModule {

    @Provides
    TransactionDetailViewModelFactory provideTransactionDetailViewModelFactory(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            GenericWalletInteract genericWalletInteract,
            ExternalBrowserRouter externalBrowserRouter,
            TokensService tokensService) {
        return new TransactionDetailViewModelFactory(
                findDefaultNetworkInteract, genericWalletInteract, externalBrowserRouter, tokensService);
    }

    @Provides
    FindDefaultNetworkInteract provideFindDefaultNetworkInteract(
            EthereumNetworkRepositoryType ethereumNetworkRepository) {
        return new FindDefaultNetworkInteract(ethereumNetworkRepository);
    }

    @Provides
    ExternalBrowserRouter externalBrowserRouter() {
        return new ExternalBrowserRouter();
    }

    @Provides
    GenericWalletInteract findDefaultWalletInteract(WalletRepositoryType walletRepository) {
        return new GenericWalletInteract(walletRepository);
    }
}
