package com.alphawallet.app.di;

import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.viewmodel.TokenAlertsViewModelFactory;
import com.alphawallet.app.viewmodel.TokenInfoViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class TokenAlertsModule {
    @Provides
    TokenAlertsViewModelFactory provideTokenAlertsViewModelFactory(AssetDefinitionService assetDefinitionService,
                                                                 TokensService tokensService)
    {
        return new TokenAlertsViewModelFactory(
                assetDefinitionService,
                tokensService);
    }
}
