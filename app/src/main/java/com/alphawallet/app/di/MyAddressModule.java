package com.alphawallet.app.di;

import dagger.Module;
import dagger.Provides;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.viewmodel.MyAddressViewModelFactory;

@Module
class MyAddressModule {
    @Provides
    MyAddressViewModelFactory provideMyAddressViewModelFactory(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            TokensService tokensService) {
        return new MyAddressViewModelFactory(
                findDefaultNetworkInteract,
                ethereumNetworkRepository,
                tokensService);
    }

    @Provides
    FindDefaultNetworkInteract provideFindDefaultNetworkInteract(
            EthereumNetworkRepositoryType ethereumNetworkRepositoryType) {
        return new FindDefaultNetworkInteract(ethereumNetworkRepositoryType);
    }
}
