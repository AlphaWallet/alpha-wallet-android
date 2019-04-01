package io.stormbird.wallet.di;

import io.stormbird.wallet.interact.AddTokenInteract;
import io.stormbird.wallet.interact.CreateWalletInteract;
import io.stormbird.wallet.interact.FetchWalletsInteract;
import io.stormbird.wallet.interact.ImportWalletInteract;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.repository.LocaleRepository;
import io.stormbird.wallet.repository.LocaleRepositoryType;
import io.stormbird.wallet.repository.PasswordStore;
import io.stormbird.wallet.repository.PreferenceRepositoryType;
import io.stormbird.wallet.repository.TokenRepositoryType;
import io.stormbird.wallet.repository.WalletRepositoryType;
import io.stormbird.wallet.viewmodel.SplashViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
public class SplashModule {

    @Provides
    SplashViewModelFactory provideSplashViewModelFactory(FetchWalletsInteract fetchWalletsInteract,
                                                         EthereumNetworkRepositoryType networkRepository,
                                                         ImportWalletInteract importWalletInteract,
                                                         AddTokenInteract addTokenInteract,
                                                         CreateWalletInteract createWalletInteract,
                                                         PreferenceRepositoryType preferenceRepository,
                                                         LocaleRepositoryType localeRepository) {
        return new SplashViewModelFactory(fetchWalletsInteract, networkRepository, importWalletInteract, addTokenInteract, createWalletInteract, preferenceRepository, localeRepository);
    }

    @Provides
    FetchWalletsInteract provideFetchWalletInteract(WalletRepositoryType walletRepository) {
        return new FetchWalletsInteract(walletRepository);
    }

    @Provides
    ImportWalletInteract provideImportWalletInteract(
            WalletRepositoryType walletRepository, PasswordStore passwordStore) {
        return new ImportWalletInteract(walletRepository, passwordStore);
    }

    @Provides
    AddTokenInteract provideAddTokenInteract(
            TokenRepositoryType tokenRepository) {
        return new AddTokenInteract(tokenRepository);
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
