package com.alphawallet.app.viewmodel;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import com.alphawallet.app.service.AssetDefinitionService;

import javax.inject.Inject;

public class TokenScriptManagementViewModelFactory implements ViewModelProvider.Factory {

    private final AssetDefinitionService assetDefinitionService;

    @Inject
    public TokenScriptManagementViewModelFactory(AssetDefinitionService assetDefinitionService) {
        this.assetDefinitionService = assetDefinitionService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new TokenScriptManagementViewModel(assetDefinitionService);
    }
}

