package com.alphawallet.app.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.alphawallet.app.interact.AddTokenInteract;
import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.interact.SetupTokensInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;

import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.AlphaWalletService;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.KeyService;

/**
 * Created by James on 9/03/2018.
 */

public class ImportTokenViewModelFactory implements ViewModelProvider.Factory {

    private final GenericWalletInteract genericWalletInteract;
    private final CreateTransactionInteract createTransactionInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final SetupTokensInteract setupTokensInteract;
    private final AlphaWalletService alphaWalletService;
    private final AddTokenInteract addTokenInteract;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final AssetDefinitionService assetDefinitionService;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final GasService gasService;
    private final KeyService keyService;

    public ImportTokenViewModelFactory(GenericWalletInteract genericWalletInteract,
                                       CreateTransactionInteract createTransactionInteract,
                                       FetchTokensInteract fetchTokensInteract,
                                       SetupTokensInteract setupTokensInteract,
                                       AlphaWalletService alphaWalletService,
                                       AddTokenInteract addTokenInteract,
                                       EthereumNetworkRepositoryType ethereumNetworkRepository,
                                       AssetDefinitionService assetDefinitionService,
                                       FetchTransactionsInteract fetchTransactionsInteract,
                                       GasService gasService,
                                       KeyService keyService) {
        this.genericWalletInteract = genericWalletInteract;
        this.createTransactionInteract = createTransactionInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.setupTokensInteract = setupTokensInteract;
        this.alphaWalletService = alphaWalletService;
        this.addTokenInteract = addTokenInteract;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.assetDefinitionService = assetDefinitionService;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.gasService = gasService;
        this.keyService = keyService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new ImportTokenViewModel(genericWalletInteract, createTransactionInteract, fetchTokensInteract, setupTokensInteract, alphaWalletService, addTokenInteract, ethereumNetworkRepository, assetDefinitionService, fetchTransactionsInteract, gasService, keyService);
    }
}

