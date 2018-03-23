package io.awallet.crypto.alphawallet.viewmodel;


import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import io.awallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import io.awallet.crypto.alphawallet.interact.GetDefaultWalletBalance;
import io.awallet.crypto.alphawallet.router.HelpRouter;
import io.awallet.crypto.alphawallet.router.MyAddressRouter;

public class NewSettingsViewModelFactory implements ViewModelProvider.Factory {
    private final MyAddressRouter myAddressRouter;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final GetDefaultWalletBalance getDefaultWalletBalance;
    private final HelpRouter helpRouter;

    public NewSettingsViewModelFactory(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            GetDefaultWalletBalance getDefaultWalletBalance,
            MyAddressRouter myAddressRouter,
            HelpRouter helpRouter) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.getDefaultWalletBalance = getDefaultWalletBalance;
        this.myAddressRouter = myAddressRouter;
        this.helpRouter = helpRouter;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new NewSettingsViewModel(
                findDefaultNetworkInteract,
                findDefaultWalletInteract,
                getDefaultWalletBalance,
                myAddressRouter,
                helpRouter
        );
    }
}
