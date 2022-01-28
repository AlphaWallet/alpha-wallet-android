package com.alphawallet.app.viewmodel;

import com.alphawallet.app.interact.GenericWalletInteract;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class WalletConnectV2ViewModelFactory implements ViewModelProvider.Factory
{
    private final GenericWalletInteract genericWalletInteract;

    @Inject
    public WalletConnectV2ViewModelFactory(GenericWalletInteract genericWalletInteract)
    {
        this.genericWalletInteract = genericWalletInteract;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass)
    {
        return (T) new WalletConnectV2ViewModel(genericWalletInteract);
    }
}
