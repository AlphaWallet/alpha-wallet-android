package com.alphawallet.app.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.alphawallet.app.interact.AddTokenInteract;
import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.interact.SetupTokensInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;

import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;

public class AddTokenViewModelFactory implements ViewModelProvider.Factory {

    private final AddTokenInteract addTokenInteract;
    private final GenericWalletInteract genericWalletInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final SetupTokensInteract setupTokensInteract;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;

    public AddTokenViewModelFactory(
            AddTokenInteract addTokenInteract,
            GenericWalletInteract genericWalletInteract,
            FetchTokensInteract fetchTokensInteract,
            SetupTokensInteract setupTokensInteract,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            FetchTransactionsInteract fetchTransactionsInteract,
            AssetDefinitionService assetDefinitionService,
            TokensService tokensService) {
        this.addTokenInteract = addTokenInteract;
        this.genericWalletInteract = genericWalletInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.setupTokensInteract = setupTokensInteract;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new AddTokenViewModel(addTokenInteract, genericWalletInteract, fetchTokensInteract, setupTokensInteract, ethereumNetworkRepository, fetchTransactionsInteract, assetDefinitionService, tokensService);
    }
}
