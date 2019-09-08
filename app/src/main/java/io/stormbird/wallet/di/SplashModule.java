package io.stormbird.wallet.di;

import dagger.Module;
import dagger.Provides;
import io.stormbird.wallet.interact.FetchWalletsInteract;
import io.stormbird.wallet.repository.*;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.KeyService;
import io.stormbird.wallet.viewmodel.SplashViewModelFactory;

@Module
public class SplashModule {

    @Provides
    SplashViewModelFactory provideSplashViewModelFactory(FetchWalletsInteract fetchWalletsInteract,
                                                         PreferenceRepositoryType preferenceRepository,
                                                         LocaleRepositoryType localeRepository,
                                                         KeyService keyService,
                                                         AssetDefinitionService assetDefinitionService) {
        return new SplashViewModelFactory(fetchWalletsInteract, preferenceRepository, localeRepository, keyService, assetDefinitionService);
    }

    @Provides
    FetchWalletsInteract provideFetchWalletInteract(WalletRepositoryType walletRepository) {
        return new FetchWalletsInteract(walletRepository);
    }

    @Provides
    LocaleRepositoryType provideLocaleRepositoryType(PreferenceRepositoryType preferenceRepository) {
        return new LocaleRepository(preferenceRepository);
    }
}
