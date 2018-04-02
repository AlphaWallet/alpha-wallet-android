package io.awallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;

import io.awallet.crypto.alphawallet.interact.FetchGasSettingsInteract;
import io.awallet.crypto.alphawallet.router.ConfirmationRouter;

import io.awallet.crypto.alphawallet.router.MyAddressRouter;
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
