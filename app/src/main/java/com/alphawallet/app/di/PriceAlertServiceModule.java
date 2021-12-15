package com.alphawallet.app.di;

import android.content.Context;

import androidx.room.PrimaryKey;

import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.repository.WalletRepositoryType;
import com.alphawallet.app.router.TokenDetailRouter;
import com.alphawallet.app.service.AnalyticsServiceType;
import com.alphawallet.app.service.OpenSeaService;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.viewmodel.NameThisWalletViewModelFactory;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
class PriceAlertServiceModule {

    @Provides
    GenericWalletInteract provideGenericWalletInteract(WalletRepositoryType walletRepository) {
        return new GenericWalletInteract(walletRepository);
    }

    @Provides
    TokenDetailRouter provideTokenDetailRouter() {
        return new TokenDetailRouter();
    }
}
