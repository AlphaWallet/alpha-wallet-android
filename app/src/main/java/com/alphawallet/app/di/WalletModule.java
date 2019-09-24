package com.alphawallet.app.di;


import com.alphawallet.app.interact.AddTokenInteract;
import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.interact.SetupTokensInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.repository.TransactionRepositoryType;
import com.alphawallet.app.repository.WalletRepositoryType;
import com.alphawallet.app.router.AddTokenRouter;
import com.alphawallet.app.router.AssetDisplayRouter;
import com.alphawallet.app.router.Erc20DetailRouter;
import com.alphawallet.app.router.SendTokenRouter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.OpenseaService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.viewmodel.WalletViewModelFactory;

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
