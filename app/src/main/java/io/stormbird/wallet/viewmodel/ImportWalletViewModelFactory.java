package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import io.stormbird.wallet.interact.ImportWalletInteract;
import io.stormbird.wallet.service.GasService;
import io.stormbird.wallet.service.KeyService;

public class ImportWalletViewModelFactory implements ViewModelProvider.Factory {

    private final ImportWalletInteract importWalletInteract;
    private final KeyService keyService;
    private final GasService gasService;

    public ImportWalletViewModelFactory(ImportWalletInteract importWalletInteract, KeyService keyService, GasService gasService) {
        this.importWalletInteract = importWalletInteract;
        this.keyService = keyService;
        this.gasService = gasService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new ImportWalletViewModel(importWalletInteract, keyService, gasService);
    }
}
