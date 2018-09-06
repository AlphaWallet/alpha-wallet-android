package io.stormbird.wallet.viewmodel;


import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import io.stormbird.wallet.interact.CreateTransactionInteract;
import io.stormbird.wallet.interact.FetchGasSettingsInteract;
import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.service.AssetDefinitionService;

public class DappBrowserViewModelFactory implements ViewModelProvider.Factory {
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final CreateTransactionInteract createTransactionInteract;
    private final FetchGasSettingsInteract fetchGasSettingsInteract;
    private final FetchTokensInteract fetchTokensInteract;

    public DappBrowserViewModelFactory(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            AssetDefinitionService assetDefinitionService,
            CreateTransactionInteract createTransactionInteract,
            FetchGasSettingsInteract fetchGasSettingsInteract,
            FetchTokensInteract fetchTokensInteract) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.createTransactionInteract = createTransactionInteract;
        this.fetchGasSettingsInteract = fetchGasSettingsInteract;
        this.fetchTokensInteract = fetchTokensInteract;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new DappBrowserViewModel(
                findDefaultNetworkInteract,
                findDefaultWalletInteract,
                assetDefinitionService,
                createTransactionInteract,
                fetchGasSettingsInteract,
                fetchTokensInteract);
    }
}
