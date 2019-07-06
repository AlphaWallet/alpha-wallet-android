package io.stormbird.wallet.di;

import dagger.Module;
import dagger.Provides;
import io.stormbird.wallet.interact.*;
import io.stormbird.wallet.interact.ENSInteract;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.repository.TokenRepositoryType;
import io.stormbird.wallet.repository.TransactionRepositoryType;
import io.stormbird.wallet.router.ConfirmationRouter;
import io.stormbird.wallet.router.MyAddressRouter;
import io.stormbird.wallet.service.GasService;
import io.stormbird.wallet.service.TokensService;
import io.stormbird.wallet.viewmodel.SendViewModelFactory;

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
