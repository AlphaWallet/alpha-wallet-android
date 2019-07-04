package io.stormbird.wallet.di;

import dagger.Module;
import dagger.Provides;
import io.stormbird.wallet.interact.CreateWalletInteract;
import io.stormbird.wallet.interact.FetchWalletsInteract;
import io.stormbird.wallet.repository.*;
import io.stormbird.wallet.viewmodel.SplashViewModelFactory;

@Module
public class SplashModule {

    @Provides
    SplashViewModelFactory provideSplashViewModelFactory(FetchWalletsInteract fetchWalletsInteract,
                                                         CreateWalletInteract createWalletInteract,
                                                         PreferenceRepositoryType preferenceRepository,
                                                         LocaleRepositoryType localeRepository) {
        return new SplashViewModelFactory(fetchWalletsInteract, createWalletInteract, preferenceRepository, localeRepository);
    }

    @Provides
    FetchWalletsInteract provideFetchWalletInteract(WalletRepositoryType walletRepository) {
        return new FetchWalletsInteract(walletRepository);
    }

    @Provides
    CreateWalletInteract provideCreateAccountInteract(
            WalletRepositoryType accountRepository, PasswordStore passwordStore) {
        return new CreateWalletInteract(accountRepository, passwordStore);
    }

    @Provides
    LocaleRepositoryType provideLocaleRepositoryType(PreferenceRepositoryType preferenceRepository) {
        return new LocaleRepository(preferenceRepository);
    }
}
