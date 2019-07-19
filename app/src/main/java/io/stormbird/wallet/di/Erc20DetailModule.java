package io.stormbird.wallet.di;

import dagger.Module;
import dagger.Provides;
import io.stormbird.wallet.interact.*;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.repository.TokenRepositoryType;
import io.stormbird.wallet.repository.TransactionRepositoryType;
import io.stormbird.wallet.repository.WalletRepositoryType;
import io.stormbird.wallet.router.MyAddressRouter;
import io.stormbird.wallet.router.TransactionDetailRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.TokensService;
import io.stormbird.wallet.viewmodel.Erc20DetailViewModelFactory;

@Module
class Erc20DetailModule {
    @Provides
    Erc20DetailViewModelFactory provideErc20DetailViewModelFactory(MyAddressRouter myAddressRouter,
                                                                   FetchTokensInteract fetchTokensInteract,
                                                                   FetchTransactionsInteract fetchTransactionsInteract,
                                                                   FindDefaultNetworkInteract findDefaultNetworkInteract,
                                                                   GenericWalletInteract genericWalletInteract,
                                                                   TransactionDetailRouter transactionDetailRouter,
                                                                   AssetDefinitionService assetDefinitionService,
                                                                   TokensService tokensService,
                                                                   AddTokenInteract addTokenInteract) {
        return new Erc20DetailViewModelFactory(myAddressRouter,
                fetchTokensInteract,
                fetchTransactionsInteract,
                findDefaultNetworkInteract,
                                               genericWalletInteract,
                transactionDetailRouter,
                assetDefinitionService,
                tokensService, addTokenInteract);
    }

    @Provides
    MyAddressRouter provideMyAddressRouter() {
        return new MyAddressRouter();
    }

    @Provides
    FetchTokensInteract provideFetchTokensInteract(TokenRepositoryType tokenRepository) {
        return new FetchTokensInteract(tokenRepository);
    }

    @Provides
    FindDefaultNetworkInteract provideFindDefaultNetworkInteract(
            EthereumNetworkRepositoryType ethereumNetworkRepositoryType) {
        return new FindDefaultNetworkInteract(ethereumNetworkRepositoryType);
    }

    @Provides
    GenericWalletInteract provideFindDefaultWalletInteract(WalletRepositoryType walletRepository) {
        return new GenericWalletInteract(walletRepository);
    }

    @Provides
    TransactionDetailRouter provideTransactionDetailRouter() {
        return new TransactionDetailRouter();
    }

    @Provides
    FetchTransactionsInteract provideFetchTransactionsInteract(TransactionRepositoryType transactionRepositoryType,
                                                               TokenRepositoryType tokenRepositoryType) {
        return new FetchTransactionsInteract(transactionRepositoryType, tokenRepositoryType);
    }

    @Provides
    AddTokenInteract provideAddTokenInteract(
            TokenRepositoryType tokenRepository) {
        return new AddTokenInteract(tokenRepository);
    }
}
