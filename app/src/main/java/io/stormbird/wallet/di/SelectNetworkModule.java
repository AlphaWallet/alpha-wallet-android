package io.stormbird.wallet.di;

import dagger.Module;
import dagger.Provides;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.service.TokensService;
import io.stormbird.wallet.viewmodel.SelectNetworkViewModelFactory;

@Module
class SelectNetworkModule {
    @Provides
    SelectNetworkViewModelFactory provideSelectNetworkViewModelFactory(EthereumNetworkRepositoryType networkRepositoryType,
                                                                       TokensService tokensService) {
        return new SelectNetworkViewModelFactory(networkRepositoryType, tokensService);
    }
}
