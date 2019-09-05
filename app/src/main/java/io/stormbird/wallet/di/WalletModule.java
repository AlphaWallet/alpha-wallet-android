package io.stormbird.wallet.di;


import io.stormbird.wallet.interact.*;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.repository.TokenRepositoryType;
import io.stormbird.wallet.repository.TransactionRepositoryType;
import io.stormbird.wallet.repository.WalletRepositoryType;
import io.stormbird.wallet.router.AddTokenRouter;
import io.stormbird.wallet.router.AssetDisplayRouter;
import io.stormbird.wallet.router.Erc20DetailRouter;
import io.stormbird.wallet.router.SendTokenRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.OpenseaService;
import io.stormbird.wallet.service.TokensService;
import io.stormbird.wallet.viewmodel.WalletViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
public class WalletModule {
    @Provides
    WalletViewModelFactory provideWalletViewModelFactory(
            FetchTokensInteract fetchTokensInteract,
            AddTokenRouter addTokenRouter,
            SendTokenRouter sendTokenRouter,
            Erc20DetailRouter erc20DetailRouter,
            AssetDisplayRouter assetDisplayRouter,
            GenericWalletInteract genericWalletInteract,
            AddTokenInteract addTokenInteract,
            SetupTokensInteract setupTokensInteract,
            AssetDefinitionService assetDefinitionService,
            TokensService tokensService,
            OpenseaService openseaService,
            FetchTransactionsInteract fetchTransactionsInteract,
            EthereumNetworkRepositoryType ethereumNetworkRepository) {
        return new WalletViewModelFactory(
                fetchTokensInteract,
                addTokenRouter,
                sendTokenRouter,
                erc20DetailRouter,
                assetDisplayRouter,
                genericWalletInteract,
                addTokenInteract,
                setupTokensInteract,
                assetDefinitionService,
                tokensService,
                openseaService,
                fetchTransactionsInteract,
                ethereumNetworkRepository);
    }

    @Provides
    FetchTokensInteract provideFetchTokensInteract(TokenRepositoryType tokenRepository) {
        return new FetchTokensInteract(tokenRepository);
    }

    @Provides
    AddTokenRouter provideAddTokenRouter() {
        return new AddTokenRouter();
    }

    @Provides
    SendTokenRouter provideSendTokenRouter() {
        return new SendTokenRouter();
    }

    @Provides
    Erc20DetailRouter provideErc20DetailRouterRouter() {
        return new Erc20DetailRouter();
    }

    @Provides
    AssetDisplayRouter provideRedeemTokenRouter() {
        return new AssetDisplayRouter();
    }

    @Provides
    AddTokenInteract provideAddTokenInteract(
            TokenRepositoryType tokenRepository) {
        return new AddTokenInteract(tokenRepository);
    }

    @Provides
    GenericWalletInteract provideFindDefaultWalletInteract(WalletRepositoryType walletRepository) {
        return new GenericWalletInteract(walletRepository);
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
}
