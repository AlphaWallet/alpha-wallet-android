package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import io.stormbird.wallet.interact.ImportWalletInteract;

public class ImportWalletViewModelFactory implements ViewModelProvider.Factory {

    private final ImportWalletInteract importWalletInteract;

    public ImportWalletViewModelFactory(ImportWalletInteract importWalletInteract) {
        this.importWalletInteract = importWalletInteract;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new ImportWalletViewModel(importWalletInteract);
    }
}
