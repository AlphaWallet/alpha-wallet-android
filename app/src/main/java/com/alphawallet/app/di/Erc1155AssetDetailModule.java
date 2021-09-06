package com.alphawallet.app.di;

import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.WalletRepositoryType;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.viewmodel.Erc1155AssetDetailViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class Erc1155AssetDetailModule {
    @Provides
    Erc1155AssetDetailViewModelFactory provideErc1155AssetDetailViewModelFactory(
            AssetDefinitionService assetDefinitionService,
            TokensService tokensService,
            GenericWalletInteract walletInteract)
    {
        return new Erc1155AssetDetailViewModelFactory(assetDefinitionService, tokensService, walletInteract);
    }

    @Provides
    GenericWalletInteract provideFindDefaultWalletInteract(WalletRepositoryType walletRepository)
    {
        return new GenericWalletInteract(walletRepository);
    }
}
