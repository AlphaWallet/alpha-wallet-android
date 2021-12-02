package com.alphawallet.app.di;

import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.viewmodel.MyAddressViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class MyAddressModule {
    @Provides
    MyAddressViewModelFactory provideMyAddressViewModelFactory(
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            TokensService tokensService,
            AssetDefinitionService assetDefinitionService) {
        return new MyAddressViewModelFactory(
                ethereumNetworkRepository,
                tokensService,
                assetDefinitionService);
    }
}
