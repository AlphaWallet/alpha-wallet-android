package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;

import io.stormbird.wallet.interact.FetchGasSettingsInteract;
import io.stormbird.wallet.router.ConfirmationRouter;

import io.stormbird.wallet.router.MyAddressRouter;
import io.reactivex.annotations.NonNull;

public class SendViewModelFactory implements ViewModelProvider.Factory {

    private final ConfirmationRouter confirmationRouter;
    private final FetchGasSettingsInteract fetchGasSettingsInteract;
    private final MyAddressRouter myAddressRouter;

    public SendViewModelFactory(ConfirmationRouter confirmationRouter,
                                FetchGasSettingsInteract fetchGasSettingsInteract,
                                MyAddressRouter myAddressRouter) {
        this.confirmationRouter = confirmationRouter;
        this.fetchGasSettingsInteract = fetchGasSettingsInteract;
        this.myAddressRouter = myAddressRouter;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new SendViewModel(confirmationRouter, fetchGasSettingsInteract, myAddressRouter);
    }
}
