package com.alphawallet.app.di;

import dagger.Module;
import dagger.Provides;
import com.alphawallet.app.interact.FetchWalletsInteract;

import com.alphawallet.app.repository.CurrencyRepository;
import com.alphawallet.app.repository.CurrencyRepositoryType;
import com.alphawallet.app.repository.LocaleRepository;
import com.alphawallet.app.repository.LocaleRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.repository.WalletRepositoryType;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.viewmodel.SplashViewModelFactory;

@Module
public class SplashModule {

    @Provides
    SplashViewModelFactory provideSplashViewModelFactory(FetchWalletsInteract fetchWalletsInteract,
                                                         PreferenceRepositoryType preferenceRepository,
                                                         LocaleRepositoryType localeRepository,
                                                         KeyService keyService,
                                                         AssetDefinitionService assetDefinitionService,
                                                         CurrencyRepositoryType currencyRepository) {
        return new SplashViewModelFactory(
                fetchWalletsInteract,
                preferenceRepository,
                localeRepository,
                keyService,
                assetDefinitionService,
                currencyRepository);
    }

    @Provides
    FetchWalletsInteract provideFetchWalletInteract(WalletRepositoryType walletRepository) {
        return new FetchWalletsInteract(walletRepository);
    }

    @Provides
    LocaleRepositoryType provideLocaleRepositoryType(PreferenceRepositoryType preferenceRepository) {
        return new LocaleRepository(preferenceRepository);
    }

    @Provides
    CurrencyRepositoryType provideCurrencyRepositoryType(PreferenceRepositoryType preferenceRepository) {
        return new CurrencyRepository(preferenceRepository);
    }
}
