package com.alphawallet.app.di;

import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.repository.TransactionRepositoryType;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.viewmodel.TokenActivityViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class TokenActivityModule {
    @Provides
    TokenActivityViewModelFactory provideTokenActivityViewModelFactory(AssetDefinitionService assetDefinitionService,
                                                                       FetchTransactionsInteract fetchTransactionsInteract,
                                                                       TokensService tokensService)
    {
        return new TokenActivityViewModelFactory(
                assetDefinitionService,
                fetchTransactionsInteract,
                tokensService);
    }

    @Provides
    FetchTransactionsInteract provideFetchTransactionsInteract(TransactionRepositoryType transactionRepositoryType,
                                                               TokenRepositoryType tokenRepositoryType)
    {
        return new FetchTransactionsInteract(transactionRepositoryType, tokenRepositoryType);
    }
}
