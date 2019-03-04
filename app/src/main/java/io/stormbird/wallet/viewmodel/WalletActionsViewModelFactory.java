package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import javax.inject.Inject;

import io.stormbird.wallet.interact.FetchWalletsInteract;

public class WalletActionsViewModelFactory implements ViewModelProvider.Factory {
    private final FetchWalletsInteract fetchWalletsInteract;

    @Inject
    public WalletActionsViewModelFactory(FetchWalletsInteract fetchWalletsInteract) {
        this.fetchWalletsInteract = fetchWalletsInteract;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new WalletActionsViewModel(fetchWalletsInteract);
    }
}
