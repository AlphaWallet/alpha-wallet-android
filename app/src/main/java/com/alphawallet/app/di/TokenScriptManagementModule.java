package com.alphawallet.app.di;

import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.viewmodel.TokenScriptManagementViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class TokenScriptManagementModule {
    @Provides
    TokenScriptManagementViewModelFactory tokenScriptManagementViewModelFactory(AssetDefinitionService assetDefinitionService)
    {
        return new TokenScriptManagementViewModelFactory(assetDefinitionService);
    }
}
