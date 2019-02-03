package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;
import io.stormbird.wallet.interact.*;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.FeeMasterService;

/**
 * Created by James on 9/03/2018.
 */

public class ImportTokenViewModelFactory implements ViewModelProvider.Factory {

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final CreateTransactionInteract createTransactionInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final SetupTokensInteract setupTokensInteract;
    private final FeeMasterService feeMasterService;
    private final AddTokenInteract addTokenInteract;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final AssetDefinitionService assetDefinitionService;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final FetchGasSettingsInteract fetchGasSettingsInteract;

    public ImportTokenViewModelFactory(FindDefaultNetworkInteract findDefaultNetworkInteract,
                                       FindDefaultWalletInteract findDefaultWalletInteract,
                                       CreateTransactionInteract createTransactionInteract,
                                       FetchTokensInteract fetchTokensInteract,
                                       SetupTokensInteract setupTokensInteract,
                                       FeeMasterService feeMasterService,
                                       AddTokenInteract addTokenInteract,
                                       EthereumNetworkRepositoryType ethereumNetworkRepository,
                                       AssetDefinitionService assetDefinitionService,
                                       FetchTransactionsInteract fetchTransactionsInteract,
                                       FetchGasSettingsInteract fetchGasSettingsInteract) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.createTransactionInteract = createTransactionInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.setupTokensInteract = setupTokensInteract;
        this.feeMasterService = feeMasterService;
        this.addTokenInteract = addTokenInteract;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.assetDefinitionService = assetDefinitionService;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.fetchGasSettingsInteract = fetchGasSettingsInteract;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new ImportTokenViewModel(findDefaultNetworkInteract, findDefaultWalletInteract, createTransactionInteract, fetchTokensInteract, setupTokensInteract, feeMasterService, addTokenInteract, ethereumNetworkRepository, assetDefinitionService, fetchTransactionsInteract, fetchGasSettingsInteract);
    }
}

