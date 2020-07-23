package com.alphawallet.app.di;

import com.alphawallet.app.interact.AddTokenInteract;
import com.alphawallet.app.interact.ENSInteract;
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

import dagger.Module;
import dagger.Provides;

@Module
class SendModule
{
    @Provides
    SendViewModelFactory provideSendViewModelFactory(ConfirmationRouter confirmationRouter,
                                                     MyAddressRouter myAddressRouter,
                                                     ENSInteract ensInteract,
                                                     EthereumNetworkRepositoryType networkRepositoryType,
                                                     TokensService tokensService,
                                                     GasService gasService,
                                                     SetupTokensInteract setupTokensInteract,
                                                     FetchTransactionsInteract fetchTransactionsInteract,
                                                     AddTokenInteract addTokenInteract)
    {
        return new SendViewModelFactory(confirmationRouter,
                myAddressRouter,
                ensInteract,
                networkRepositoryType,
                tokensService,
                gasService,
                setupTokensInteract,
                fetchTransactionsInteract,
                addTokenInteract);
    }

    @Provides
    ConfirmationRouter provideConfirmationRouter()
    {
        return new ConfirmationRouter();
    }

    @Provides
    MyAddressRouter provideMyAddressRouter()
    {
        return new MyAddressRouter();
    }

    @Provides
    ENSInteract provideENSInteract(TokenRepositoryType tokenRepository)
    {
        return new ENSInteract(tokenRepository);
    }

    @Provides
    AddTokenInteract provideAddTokenInteract(
            TokenRepositoryType tokenRepository)
    {
        return new AddTokenInteract(tokenRepository);
    }

    @Provides
    SetupTokensInteract provideSetupTokensInteract(TokenRepositoryType tokenRepository)
    {
        return new SetupTokensInteract(tokenRepository);
    }

    @Provides
    FetchTransactionsInteract provideFetchTransactionsInteract(TransactionRepositoryType transactionRepository,
                                                               TokenRepositoryType tokenRepositoryType)
    {
        return new FetchTransactionsInteract(transactionRepository, tokenRepositoryType);
    }
}
