package io.stormbird.wallet.di;

import io.stormbird.wallet.interact.FetchTransactionsInteract;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.interact.GetDefaultWalletBalance;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.repository.TransactionRepositoryType;
import io.stormbird.wallet.repository.WalletRepositoryType;
import io.stormbird.wallet.router.AddTokenRouter;
import io.stormbird.wallet.router.ExternalBrowserRouter;
import io.stormbird.wallet.router.HelpRouter;
import io.stormbird.wallet.router.ManageWalletsRouter;
import io.stormbird.wallet.router.MarketBrowseRouter;
import io.stormbird.wallet.router.MarketplaceRouter;
import io.stormbird.wallet.router.MyAddressRouter;
import io.stormbird.wallet.router.MyTokensRouter;
import io.stormbird.wallet.router.NewSettingsRouter;
import io.stormbird.wallet.router.SendRouter;
import io.stormbird.wallet.router.SettingsRouter;
import io.stormbird.wallet.router.TransactionDetailRouter;
import io.stormbird.wallet.router.WalletRouter;
import io.stormbird.wallet.viewmodel.HomeViewModelFactory;

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
