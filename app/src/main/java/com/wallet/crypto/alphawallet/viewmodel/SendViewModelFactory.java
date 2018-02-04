package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;

import com.wallet.crypto.alphawallet.interact.FetchGasSettingsInteract;
import com.wallet.crypto.alphawallet.router.ConfirmationRouter;

import io.reactivex.annotations.NonNull;

public class SendViewModelFactory implements ViewModelProvider.Factory {

    private final ConfirmationRouter confirmationRouter;
    private final FetchGasSettingsInteract fetchGasSettingsInteract;

    public SendViewModelFactory(ConfirmationRouter confirmationRouter, FetchGasSettingsInteract fetchGasSettingsInteract) {
        this.confirmationRouter = confirmationRouter;
        this.fetchGasSettingsInteract = fetchGasSettingsInteract;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new SendViewModel(confirmationRouter, fetchGasSettingsInteract);
    }
}
