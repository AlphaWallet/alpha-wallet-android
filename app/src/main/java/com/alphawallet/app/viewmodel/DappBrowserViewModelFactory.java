package com.alphawallet.app.viewmodel;


import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;

import com.alphawallet.app.router.ConfirmationRouter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.KeyService;

public class DappBrowserViewModelFactory implements ViewModelProvider.Factory {
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final GenericWalletInteract genericWalletInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final CreateTransactionInteract createTransactionInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final ConfirmationRouter confirmationRouter;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final GasService gasService;
    private final KeyService keyService;

    public DappBrowserViewModelFactory(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            GenericWalletInteract genericWalletInteract,
            AssetDefinitionService assetDefinitionService,
            CreateTransactionInteract createTransactionInteract,
            FetchTokensInteract fetchTokensInteract,
            ConfirmationRouter confirmationRouter,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            GasService gasService,
            KeyService keyService) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.genericWalletInteract = genericWalletInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.createTransactionInteract = createTransactionInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.confirmationRouter = confirmationRouter;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.gasService = gasService;
        this.keyService = keyService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new DappBrowserViewModel(
                findDefaultNetworkInteract,
                genericWalletInteract,
                assetDefinitionService,
                createTransactionInteract,
                fetchTokensInteract,
                confirmationRouter,
                ethereumNetworkRepository,
                gasService,
                keyService);
    }
}
