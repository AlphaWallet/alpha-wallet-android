package com.alphawallet.app.di;

import com.alphawallet.app.interact.AddTokenInteract;
import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.repository.TransactionRepositoryType;
import com.alphawallet.app.router.ConfirmationRouter;
import com.alphawallet.app.router.MyAddressRouter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.GasService2;
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
                                                     AddTokenInteract addTokenInteract,
                                                     CreateTransactionInteract createTransactionInteract,
                                                     GasService2 gasService,
                                                     AssetDefinitionService assetDefinitionService,
                                                     KeyService keyService) {
        return new SendViewModelFactory(myAddressRouter,
                networkRepositoryType,
                tokensService,
                fetchTransactionsInteract,
                addTokenInteract,
                createTransactionInteract,
                gasService,
                assetDefinitionService,
                keyService);
    }

    @Provides
    MyAddressRouter provideMyAddressRouter() {
        return new MyAddressRouter();
    }

    @Provides
    AddTokenInteract provideAddTokenInteract(
            TokenRepositoryType tokenRepository) {
        return new AddTokenInteract(tokenRepository);
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
