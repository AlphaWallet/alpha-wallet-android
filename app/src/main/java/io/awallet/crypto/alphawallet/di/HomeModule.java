package io.awallet.crypto.alphawallet.di;

import io.awallet.crypto.alphawallet.interact.FetchTransactionsInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import io.awallet.crypto.alphawallet.interact.GetDefaultWalletBalance;
import io.awallet.crypto.alphawallet.repository.EthereumNetworkRepositoryType;
import io.awallet.crypto.alphawallet.repository.TransactionRepositoryType;
import io.awallet.crypto.alphawallet.repository.WalletRepositoryType;
import io.awallet.crypto.alphawallet.router.AddTokenRouter;
import io.awallet.crypto.alphawallet.router.ExternalBrowserRouter;
import io.awallet.crypto.alphawallet.router.HelpRouter;
import io.awallet.crypto.alphawallet.router.ManageWalletsRouter;
import io.awallet.crypto.alphawallet.router.MarketBrowseRouter;
import io.awallet.crypto.alphawallet.router.MarketplaceRouter;
import io.awallet.crypto.alphawallet.router.MyAddressRouter;
import io.awallet.crypto.alphawallet.router.MyTokensRouter;
import io.awallet.crypto.alphawallet.router.NewSettingsRouter;
import io.awallet.crypto.alphawallet.router.SendRouter;
import io.awallet.crypto.alphawallet.router.SettingsRouter;
import io.awallet.crypto.alphawallet.router.TransactionDetailRouter;
import io.awallet.crypto.alphawallet.router.WalletRouter;
import io.awallet.crypto.alphawallet.viewmodel.HomeViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class HomeModule {
    @Provides
    HomeViewModelFactory provideTransactionsViewModelFactory(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            FetchTransactionsInteract fetchTransactionsInteract,
            GetDefaultWalletBalance getDefaultWalletBalance,
            ManageWalletsRouter manageWalletsRouter,
            SettingsRouter settingsRouter,
            SendRouter sendRouter,
            TransactionDetailRouter transactionDetailRouter,
            MyAddressRouter myAddressRouter,
            MyTokensRouter myTokensRouter,
            ExternalBrowserRouter externalBrowserRouter,
            MarketBrowseRouter marketBrowseRouter,
            WalletRouter walletRouter,
            MarketplaceRouter marketplaceRouter,
            NewSettingsRouter newSettingsRouter,
            AddTokenRouter addTokenRouter,
            HelpRouter helpRouter) {
        return new HomeViewModelFactory(
                findDefaultNetworkInteract,
                findDefaultWalletInteract,
                fetchTransactionsInteract,
                getDefaultWalletBalance,
                manageWalletsRouter,
                settingsRouter,
                sendRouter,
                transactionDetailRouter,
                myAddressRouter,
                myTokensRouter,
                externalBrowserRouter,
                marketBrowseRouter,
                walletRouter,
                marketplaceRouter,
                newSettingsRouter,
                addTokenRouter,
                helpRouter);
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
    GetDefaultWalletBalance provideGetDefaultWalletBalance(
            WalletRepositoryType walletRepository, EthereumNetworkRepositoryType ethereumNetworkRepository) {
        return new GetDefaultWalletBalance(walletRepository, ethereumNetworkRepository);
    }

    @Provides
    ManageWalletsRouter provideManageWalletsRouter() {
        return new ManageWalletsRouter();
    }

    @Provides
    SettingsRouter provideSettingsRouter() {
        return new SettingsRouter();
    }

    @Provides
    AddTokenRouter provideAddTokenRouter() {
        return new AddTokenRouter();
    }

    @Provides
    SendRouter provideSendRouter() { return new SendRouter(); }

    @Provides
    TransactionDetailRouter provideTransactionDetailRouter() {
        return new TransactionDetailRouter();
    }

    @Provides
    MyAddressRouter provideMyAddressRouter() {
        return new MyAddressRouter();
    }

    @Provides
    MyTokensRouter provideMyTokensRouter() {
        return new MyTokensRouter();
    }

    @Provides
    ExternalBrowserRouter provideExternalBrowserRouter() {
        return new ExternalBrowserRouter();
    }

    @Provides
    MarketBrowseRouter provideMarketBrowseRouter() { return new MarketBrowseRouter(); }

    @Provides
    WalletRouter providesWalletRouter() { return new WalletRouter(); }

    @Provides
    MarketplaceRouter providesMarketplaceRouter() { return new MarketplaceRouter(); }

    @Provides
    NewSettingsRouter providesNewSettingsRouter() { return new NewSettingsRouter(); }

    @Provides
    HelpRouter providesHelpRouter() { return new HelpRouter(); }
}
