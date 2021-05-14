package com.alphawallet.app.di;

import dagger.Module;
import dagger.Provides;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.viewmodel.MyAddressViewModelFactory;

@Module
class MyAddressModule {
    @Provides
    MyAddressViewModelFactory provideMyAddressViewModelFactory(
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            TokensService tokensService) {
        return new MyAddressViewModelFactory(
                ethereumNetworkRepository,
                tokensService);
    }
}
