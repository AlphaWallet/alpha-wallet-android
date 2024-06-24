package com.langitwallet.app.di;

import com.langitwallet.app.interact.GenericWalletInteract;
import com.langitwallet.app.repository.WalletRepositoryType;
import com.langitwallet.app.router.TokenDetailRouter;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ServiceComponent;


@Module
@InstallIn(ServiceComponent.class)
/** A module to provide dependencies to services */
public class ServiceModule {

    @Provides
    GenericWalletInteract provideGenericWalletInteract(WalletRepositoryType walletRepository)
    {
        return new GenericWalletInteract(walletRepository);
    }

    @Provides
    TokenDetailRouter provideTokenDetailRouter()
    {
        return new TokenDetailRouter();
    }
}
