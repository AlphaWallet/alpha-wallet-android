package com.alphawallet.app.di;

import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.WalletRepositoryType;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.viewmodel.NFTAssetDetailViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class NFTAssetDetailModule {
    @Provides
    NFTAssetDetailViewModelFactory provideNftAssetDetailViewModelFactory(
            AssetDefinitionService assetDefinitionService,
            TokensService tokensService,
            GenericWalletInteract walletInteract)
    {
        return new NFTAssetDetailViewModelFactory(assetDefinitionService, tokensService, walletInteract);
    }

    @Provides
    GenericWalletInteract provideFindDefaultWalletInteract(WalletRepositoryType walletRepository)
    {
        return new GenericWalletInteract(walletRepository);
    }
}
