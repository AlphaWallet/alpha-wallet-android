package com.alphawallet.app.di;

import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.repository.WalletRepositoryType;
import com.alphawallet.app.router.ExternalBrowserRouter;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.viewmodel.TransactionDetailViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
public class TransactionDetailModule {

    @Provides
    TransactionDetailViewModelFactory provideTransactionDetailViewModelFactory(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            ExternalBrowserRouter externalBrowserRouter,
            TokenRepositoryType tokenRepository,
            TokensService tokensService) {
        return new TransactionDetailViewModelFactory(
                findDefaultNetworkInteract, externalBrowserRouter, tokenRepository, tokensService);
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
