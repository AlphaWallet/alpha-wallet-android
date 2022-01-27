package com.alphawallet.app.di;

import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.WalletRepositoryType;
import com.alphawallet.app.router.TokenDetailRouter;

import dagger.Module;
import dagger.Provides;

@Module
class PriceAlertServiceModule
{

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
