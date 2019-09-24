package com.alphawallet.app.di;

import dagger.Module;
import dagger.Provides;

import com.alphawallet.app.interact.AddTokenInteract;
import com.alphawallet.app.interact.ENSInteract;
import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.interact.SetupTokensInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.repository.TransactionRepositoryType;
import com.alphawallet.app.router.ConfirmationRouter;
import com.alphawallet.app.router.MyAddressRouter;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.viewmodel.SendViewModelFactory;

@Module
class SendModule {
    @Provides
    SendViewModelFactory provideSendViewModelFactory(ConfirmationRouter confirmationRouter,
                                                     MyAddressRouter myAddressRouter,
                                                     ENSInteract ensInteract,
                                                     EthereumNetworkRepositoryType networkRepositoryType,
                                                     TokensService tokensService,
                                                     SetupTokensInteract setupTokensInteract,
                                                     FetchTransactionsInteract fetchTransactionsInteract,
                                                     AddTokenInteract addTokenInteract,
                                                     FetchTokensInteract fetchTokensInteract,
                                                     GasService gasService) {
        return new SendViewModelFactory(confirmationRouter,
                myAddressRouter,
                ensInteract,
                networkRepositoryType,
                tokensService,
                                        setupTokensInteract,
                                        fetchTransactionsInteract,
                                        addTokenInteract,
                                        fetchTokensInteract,
                gasService);
    }

    @Provides
    ConfirmationRouter provideConfirmationRouter() {
        return new ConfirmationRouter();
    }

    @Provides
    MyAddressRouter provideMyAddressRouter() {
        return new MyAddressRouter();
    }

    @Provides
    ENSInteract provideENSInteract(TokenRepositoryType tokenRepository) {
        return new ENSInteract(tokenRepository);
    }

    @Provides
    AddTokenInteract provideAddTokenInteract(
            TokenRepositoryType tokenRepository) {
        return new AddTokenInteract(tokenRepository);
    }

    @Provides
    SetupTokensInteract provideSetupTokensInteract(TokenRepositoryType tokenRepository) {
        return new SetupTokensInteract(tokenRepository);
    }

    @Provides
    FetchTransactionsInteract provideFetchTransactionsInteract(TransactionRepositoryType transactionRepository,
                                                               TokenRepositoryType tokenRepositoryType) {
        return new FetchTransactionsInteract(transactionRepository, tokenRepositoryType);
    }

    @Provides
    FetchTokensInteract provideFetchTokensInteract(TokenRepositoryType tokenRepository) {
        return new FetchTokensInteract(tokenRepository);
    }
}
