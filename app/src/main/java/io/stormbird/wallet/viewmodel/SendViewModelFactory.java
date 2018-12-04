package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;

import io.reactivex.annotations.NonNull;
import io.stormbird.wallet.interact.ENSInteract;
import io.stormbird.wallet.interact.FetchGasSettingsInteract;
import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.repository.TokenRepository;
import io.stormbird.wallet.router.ConfirmationRouter;
import io.stormbird.wallet.router.MyAddressRouter;

public class SendViewModelFactory implements ViewModelProvider.Factory {

    private final ConfirmationRouter confirmationRouter;
    private final FetchGasSettingsInteract fetchGasSettingsInteract;
    private final MyAddressRouter myAddressRouter;
    private final FetchTokensInteract fetchTokensInteract;
    private final ENSInteract ensInteract;

    public SendViewModelFactory(ConfirmationRouter confirmationRouter,
                                FetchGasSettingsInteract fetchGasSettingsInteract,
                                MyAddressRouter myAddressRouter,
                                FetchTokensInteract fetchTokensInteract,
                                ENSInteract ensInteract) {
        this.confirmationRouter = confirmationRouter;
        this.fetchGasSettingsInteract = fetchGasSettingsInteract;
        this.myAddressRouter = myAddressRouter;
        this.fetchTokensInteract = fetchTokensInteract;
        this.ensInteract = ensInteract;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new SendViewModel(confirmationRouter, fetchGasSettingsInteract, myAddressRouter, fetchTokensInteract, ensInteract);
    }
}
