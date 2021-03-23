package com.alphawallet.app.viewmodel;


import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.service.TokensService;

public class GasSettingsViewModelFactory implements ViewModelProvider.Factory {

    FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final TokensService tokensService;

    public GasSettingsViewModelFactory(FindDefaultNetworkInteract findDefaultNetworkInteract, TokensService svs) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.tokensService = svs;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new GasSettingsViewModel(findDefaultNetworkInteract, tokensService);
    }
}
