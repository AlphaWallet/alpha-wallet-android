package io.awallet.crypto.alphawallet.di;

import io.awallet.crypto.alphawallet.viewmodel.HelpViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class HelpModule {
    @Provides
    HelpViewModelFactory provideMarketplaceViewModelFactory() {
        return new HelpViewModelFactory();
    }
}
