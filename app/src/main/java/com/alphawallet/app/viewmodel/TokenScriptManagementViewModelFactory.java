package com.alphawallet.app.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

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

