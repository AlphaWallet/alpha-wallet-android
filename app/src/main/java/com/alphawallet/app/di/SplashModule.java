package com.alphawallet.app.di;

import com.alphawallet.app.interact.FetchWalletsInteract;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.repository.WalletRepositoryType;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.viewmodel.SplashViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
public class SplashModule {

    @Provides
    SplashViewModelFactory provideSplashViewModelFactory(FetchWalletsInteract fetchWalletsInteract,
                                                         PreferenceRepositoryType preferenceRepository,
                                                         KeyService keyService) {
        return new SplashViewModelFactory(
                fetchWalletsInteract,
                preferenceRepository,
                keyService);
    }

    @Provides
    FetchWalletsInteract provideFetchWalletInteract(WalletRepositoryType walletRepository) {
        return new FetchWalletsInteract(walletRepository);
    }
}
