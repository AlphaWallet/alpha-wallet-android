package com.alphawallet.app.di;

import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.repository.TransactionRepositoryType;
import com.alphawallet.app.router.MyAddressRouter;
import com.alphawallet.app.service.AnalyticsServiceType;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.viewmodel.SendViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class SendModule {

    @Provides
    SendViewModelFactory provideSendViewModelFactory(MyAddressRouter myAddressRouter,
                                                     EthereumNetworkRepositoryType networkRepositoryType,
                                                     TokensService tokensService,
                                                     FetchTransactionsInteract fetchTransactionsInteract,
                                                     CreateTransactionInteract createTransactionInteract,
                                                     GasService gasService,
                                                     AssetDefinitionService assetDefinitionService,
                                                     KeyService keyService,
                                                     AnalyticsServiceType analyticsService,
                                                     PreferenceRepositoryType preferenceRepository) {
        return new SendViewModelFactory(myAddressRouter,
                networkRepositoryType,
                tokensService,
                fetchTransactionsInteract,
                createTransactionInteract,
                gasService,
                assetDefinitionService,
                keyService,
                analyticsService,
                preferenceRepository);
    }

    @Provides
    MyAddressRouter provideMyAddressRouter() {
        return new MyAddressRouter();
    }

    @Provides
    FetchTransactionsInteract provideFetchTransactionsInteract(TransactionRepositoryType transactionRepository,
                                                               TokenRepositoryType tokenRepositoryType) {
        return new FetchTransactionsInteract(transactionRepository, tokenRepositoryType);
    }

    @Provides
    CreateTransactionInteract provideCreateTransactionInteract(TransactionRepositoryType transactionRepository)
    {
        return new CreateTransactionInteract(transactionRepository);
    }
}
