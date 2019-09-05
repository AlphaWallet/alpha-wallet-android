package io.stormbird.wallet.di;

import dagger.Module;
import dagger.Provides;
import io.stormbird.wallet.interact.GenericWalletInteract;
import io.stormbird.wallet.interact.GetDefaultWalletBalance;
import io.stormbird.wallet.repository.*;
import io.stormbird.wallet.router.HelpRouter;
import io.stormbird.wallet.router.HomeRouter;
import io.stormbird.wallet.router.ManageWalletsRouter;
import io.stormbird.wallet.router.MyAddressRouter;
import io.stormbird.wallet.service.TokensService;
import io.stormbird.wallet.viewmodel.NewSettingsViewModelFactory;

@Module
class NewSettingsModule {
    @Provides
    NewSettingsViewModelFactory provideNewSettingsViewModelFactory(
            GenericWalletInteract genericWalletInteract,
            GetDefaultWalletBalance getDefaultWalletBalance,
            MyAddressRouter myAddressRouter,
            HelpRouter helpRouter,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            ManageWalletsRouter manageWalletsRouter,
            HomeRouter homeRouter,
            PreferenceRepositoryType preferenceRepository,
            LocaleRepositoryType localeRepository,
            TokensService tokensService
    ) {
        return new NewSettingsViewModelFactory(
                genericWalletInteract,
                getDefaultWalletBalance,
                myAddressRouter,
                helpRouter,
                ethereumNetworkRepository,
                manageWalletsRouter,
                homeRouter,
                preferenceRepository,
                localeRepository,
                tokensService);
    }

    @Provides
    GenericWalletInteract provideFindDefaultWalletInteract(WalletRepositoryType walletRepository) {
        return new GenericWalletInteract(walletRepository);
    }

    @Provides
    GetDefaultWalletBalance provideGetDefaultWalletBalance(
            WalletRepositoryType walletRepository, EthereumNetworkRepositoryType ethereumNetworkRepository) {
        return new GetDefaultWalletBalance(walletRepository, ethereumNetworkRepository);
    }

    @Provides
    MyAddressRouter provideMyAddressRouter() {
        return new MyAddressRouter();
    }

    @Provides
    HelpRouter provideHelpRouter() {
        return new HelpRouter();
    }

    @Provides
    ManageWalletsRouter provideManageWalletsRouter() {
        return new ManageWalletsRouter();
    }

    @Provides
    HomeRouter provideHomeRouter() {
        return new HomeRouter();
    }

    @Provides
    LocaleRepositoryType provideLocaleRepository(PreferenceRepositoryType preferenceRepository) {
        return new LocaleRepository(preferenceRepository);
    }
}
