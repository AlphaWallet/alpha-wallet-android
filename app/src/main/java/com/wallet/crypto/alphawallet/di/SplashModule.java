package com.wallet.crypto.alphawallet.di;

import com.wallet.crypto.alphawallet.interact.FetchWalletsInteract;
import com.wallet.crypto.alphawallet.repository.WalletRepositoryType;
import com.wallet.crypto.alphawallet.viewmodel.SplashViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
public class SplashModule {

    @Provides
    SplashViewModelFactory provideSplashViewModelFactory(FetchWalletsInteract fetchWalletsInteract) {
        return new SplashViewModelFactory(fetchWalletsInteract);
    }

    @Provides
    FetchWalletsInteract provideFetchWalletInteract(WalletRepositoryType walletRepository) {
        return new FetchWalletsInteract(walletRepository);
    }
}
