package io.stormbird.wallet.viewmodel;


import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import io.stormbird.wallet.interact.*;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.router.AddTokenRouter;
import io.stormbird.wallet.router.AssetDisplayRouter;
import io.stormbird.wallet.router.Erc20DetailRouter;
import io.stormbird.wallet.router.SendTokenRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.OpenseaService;
import io.stormbird.wallet.service.TokensService;

public class WalletViewModelFactory implements ViewModelProvider.Factory {
    private final FetchTokensInteract fetchTokensInteract;
    private final AddTokenRouter addTokenRouter;
    private final SendTokenRouter sendTokenRouter;
    private final Erc20DetailRouter erc20DetailRouter;
    private final AssetDisplayRouter assetDisplayRouter;
    private final GenericWalletInteract genericWalletInteract;
    private final AddTokenInteract addTokenInteract;
    private final SetupTokensInteract setupTokensInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final OpenseaService openseaService;
    private final TokensService tokensService;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;

    public WalletViewModelFactory(FetchTokensInteract fetchTokensInteract,
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
        this.fetchTokensInteract = fetchTokensInteract;
        this.addTokenRouter = addTokenRouter;
        this.sendTokenRouter = sendTokenRouter;
        this.erc20DetailRouter = erc20DetailRouter;
        this.assetDisplayRouter = assetDisplayRouter;
        this.genericWalletInteract = genericWalletInteract;
        this.addTokenInteract = addTokenInteract;
        this.setupTokensInteract = setupTokensInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.openseaService = openseaService;
        this.tokensService = tokensService;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new WalletViewModel(
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
}
