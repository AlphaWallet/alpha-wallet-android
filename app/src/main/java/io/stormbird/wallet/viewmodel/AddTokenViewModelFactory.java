package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import io.stormbird.wallet.interact.*;
import io.stormbird.wallet.router.HomeRouter;
import io.stormbird.wallet.service.AssetDefinitionService;

public class AddTokenViewModelFactory implements ViewModelProvider.Factory {

    private final AddTokenInteract addTokenInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final HomeRouter homeRouter;
    private final SetupTokensInteract setupTokensInteract;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final AssetDefinitionService assetDefinitionService;

    public AddTokenViewModelFactory(
            AddTokenInteract addTokenInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            HomeRouter homeRouter,
            SetupTokensInteract setupTokensInteract,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            FetchTransactionsInteract fetchTransactionsInteract,
            AssetDefinitionService assetDefinitionService) {
        this.addTokenInteract = addTokenInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.homeRouter = homeRouter;
        this.setupTokensInteract = setupTokensInteract;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.assetDefinitionService = assetDefinitionService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new AddTokenViewModel(addTokenInteract, findDefaultWalletInteract, homeRouter, setupTokensInteract, findDefaultNetworkInteract, fetchTransactionsInteract, assetDefinitionService);
    }
}
