package com.alphawallet.app.di;

import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.viewmodel.SelectBrowserNetworkViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class SelectBrowserNetworkModule {
    @Provides
    SelectBrowserNetworkViewModelFactory provideSelectBrowserNetworkViewModelFactory(EthereumNetworkRepositoryType networkRepositoryType,
                                                                                     TokensService tokensService,
                                                                                     PreferenceRepositoryType preferenceRepositoryType)
    {
        return new SelectBrowserNetworkViewModelFactory(networkRepositoryType, tokensService, preferenceRepositoryType);
    }
}
