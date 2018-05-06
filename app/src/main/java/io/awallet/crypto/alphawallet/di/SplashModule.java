package io.awallet.crypto.alphawallet.di;

import io.awallet.crypto.alphawallet.interact.AddTokenInteract;
import io.awallet.crypto.alphawallet.interact.CreateWalletInteract;
import io.awallet.crypto.alphawallet.interact.FetchWalletsInteract;
import io.awallet.crypto.alphawallet.interact.ImportWalletInteract;
import io.awallet.crypto.alphawallet.repository.EthereumNetworkRepositoryType;
import io.awallet.crypto.alphawallet.repository.LanguageRepository;
import io.awallet.crypto.alphawallet.repository.LanguageRepositoryType;
import io.awallet.crypto.alphawallet.repository.PasswordStore;
import io.awallet.crypto.alphawallet.repository.PreferenceRepositoryType;
import io.awallet.crypto.alphawallet.repository.TokenRepositoryType;
import io.awallet.crypto.alphawallet.repository.WalletRepositoryType;
import io.awallet.crypto.alphawallet.viewmodel.SplashViewModelFactory;

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
                                                         LanguageRepositoryType languageRepository) {
        return new SplashViewModelFactory(fetchWalletsInteract, networkRepository, importWalletInteract, addTokenInteract, createWalletInteract, preferenceRepository, languageRepository);
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
            TokenRepositoryType tokenRepository,
            WalletRepositoryType walletRepository) {
        return new AddTokenInteract(walletRepository, tokenRepository);
    }

    @Provides
    CreateWalletInteract provideCreateAccountInteract(
            WalletRepositoryType accountRepository, PasswordStore passwordStore) {
        return new CreateWalletInteract(accountRepository, passwordStore);
    }

    @Provides
    LanguageRepositoryType provideLanguageRepositoryType(PreferenceRepositoryType preferenceRepository) {
        return new LanguageRepository(preferenceRepository);
    }
}
