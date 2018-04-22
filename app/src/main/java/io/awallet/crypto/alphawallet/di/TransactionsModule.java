package io.awallet.crypto.alphawallet.di;

import io.awallet.crypto.alphawallet.interact.AddTokenInteract;
import io.awallet.crypto.alphawallet.interact.FetchTokensInteract;
import io.awallet.crypto.alphawallet.interact.FetchTransactionsInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import io.awallet.crypto.alphawallet.interact.SetupTokensInteract;
import io.awallet.crypto.alphawallet.repository.EthereumNetworkRepositoryType;
import io.awallet.crypto.alphawallet.repository.TokenRepositoryType;
import io.awallet.crypto.alphawallet.repository.TransactionRepositoryType;
import io.awallet.crypto.alphawallet.repository.WalletRepositoryType;
import io.awallet.crypto.alphawallet.router.ExternalBrowserRouter;
import io.awallet.crypto.alphawallet.router.HomeRouter;
import io.awallet.crypto.alphawallet.router.TransactionDetailRouter;
import io.awallet.crypto.alphawallet.viewmodel.TransactionsViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class TransactionsModule {
    @Provides
    TransactionsViewModelFactory provideTransactionsViewModelFactory(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            FetchTransactionsInteract fetchTransactionsInteract,
            FetchTokensInteract fetchTokensInteract,
            SetupTokensInteract setupTokensInteract,
            AddTokenInteract addTokenInteract,
            TransactionDetailRouter transactionDetailRouter,
            ExternalBrowserRouter externalBrowserRouter,
            HomeRouter homeRouter) {
        return new TransactionsViewModelFactory(
                findDefaultNetworkInteract,
                findDefaultWalletInteract,
                fetchTransactionsInteract,
                fetchTokensInteract,
                setupTokensInteract,
                addTokenInteract,
                transactionDetailRouter,
                externalBrowserRouter,
                homeRouter);
    }

    @Provides
    FindDefaultNetworkInteract provideFindDefaultNetworkInteract(
            EthereumNetworkRepositoryType ethereumNetworkRepositoryType) {
        return new FindDefaultNetworkInteract(ethereumNetworkRepositoryType);
    }

    @Provides
    FindDefaultWalletInteract provideFindDefaultWalletInteract(WalletRepositoryType walletRepository) {
        return new FindDefaultWalletInteract(walletRepository);
    }

    @Provides
    FetchTransactionsInteract provideFetchTransactionsInteract(TransactionRepositoryType transactionRepository) {
        return new FetchTransactionsInteract(transactionRepository);
    }

    @Provides
    TransactionDetailRouter provideTransactionDetailRouter() {
        return new TransactionDetailRouter();
    }

    @Provides
    ExternalBrowserRouter provideExternalBrowserRouter() {
        return new ExternalBrowserRouter();
    }

    @Provides
    HomeRouter providesHomeRouter() { return new HomeRouter(); }

    @Provides
    AddTokenInteract provideAddTokenInteract(
            TokenRepositoryType tokenRepository,
            WalletRepositoryType walletRepository) {
        return new AddTokenInteract(walletRepository, tokenRepository);
    }

    @Provides
    SetupTokensInteract provideSetupTokensInteract(TokenRepositoryType tokenRepository) {
        return new SetupTokensInteract(tokenRepository);
    }

    @Provides
    FetchTokensInteract provideFetchTokensInteract(TokenRepositoryType tokenRepository) {
        return new FetchTokensInteract(tokenRepository);
    }
}
