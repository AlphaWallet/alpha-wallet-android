package com.alphawallet.app.viewmodel;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.service.TokensService;

import io.reactivex.annotations.NonNull;

public class TokenAlertsViewModelFactory implements ViewModelProvider.Factory {
    private final PreferenceRepositoryType preferenceRepository;
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;
    private final TickerService tickerService;

    public TokenAlertsViewModelFactory(AssetDefinitionService assetDefinitionService,
                                       PreferenceRepositoryType preferenceRepository,
                                       TokensService tokensService,
                                       TickerService tickerService)
    {
        this.assetDefinitionService = assetDefinitionService;
        this.preferenceRepository = preferenceRepository;
        this.tokensService = tokensService;
        this.tickerService = tickerService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass)
    {
        return (T) new TokenAlertsViewModel(preferenceRepository, tokensService, tickerService);
    }
}
