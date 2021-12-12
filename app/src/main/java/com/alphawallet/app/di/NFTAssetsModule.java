package com.alphawallet.app.di;

import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.repository.TransactionRepositoryType;
import com.alphawallet.app.router.MyAddressRouter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.OpenSeaService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.viewmodel.NFTAssetsViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class NFTAssetsModule {
    @Provides
    NFTAssetsViewModelFactory provideNftAssetsViewModelFactory(FetchTransactionsInteract fetchTransactionsInteract,
                                                                  AssetDefinitionService assetDefinitionService,
                                                                  TokensService tokensService,
                                                                  OpenSeaService openSeaService)
    {
        return new NFTAssetsViewModelFactory(
                fetchTransactionsInteract,
                assetDefinitionService,
                tokensService,
                openSeaService);
    }

    @Provides
    MyAddressRouter provideMyAddressRouter()
    {
        return new MyAddressRouter();
    }

    @Provides
    FetchTransactionsInteract provideFetchTransactionsInteract(TransactionRepositoryType transactionRepositoryType,
                                                               TokenRepositoryType tokenRepositoryType)
    {
        return new FetchTransactionsInteract(transactionRepositoryType, tokenRepositoryType);
    }
}
