package com.alphawallet.app.di;

import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.viewmodel.SelectNetworkFilterViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class SelectNetworkFilterModule {
    @Provides
    SelectNetworkFilterViewModelFactory provideSelectNetworkFilterViewModelFactory(EthereumNetworkRepositoryType networkRepositoryType,
                                                                                   TokensService tokensService,
                                                                                   PreferenceRepositoryType preferenceRepositoryType) {
        return new SelectNetworkFilterViewModelFactory(networkRepositoryType, tokensService, preferenceRepositoryType);
    }
}
