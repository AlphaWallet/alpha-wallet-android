package com.alphawallet.app.di;

import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.viewmodel.TokenAlertsViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class TokenAlertsModule {
    @Provides
    TokenAlertsViewModelFactory provideTokenAlertsViewModelFactory(AssetDefinitionService assetDefinitionService,
                                                                   PreferenceRepositoryType preferenceRepository,
                                                                   TokensService tokensService,
                                                                   TickerService tickerService)
    {
        return new TokenAlertsViewModelFactory(
                assetDefinitionService,
                preferenceRepository,
                tokensService,
                tickerService);
    }
}
