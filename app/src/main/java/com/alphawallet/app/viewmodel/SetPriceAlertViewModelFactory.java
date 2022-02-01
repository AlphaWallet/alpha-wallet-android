package com.alphawallet.app.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.repository.CurrencyRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.service.TokensService;

public class SetPriceAlertViewModelFactory implements ViewModelProvider.Factory {
    private final CurrencyRepositoryType currencyRepository;
    private final TokensService tokensService;

    public SetPriceAlertViewModelFactory(
            CurrencyRepositoryType currencyRepository,
            TokensService tokensService)
    {
        this.currencyRepository = currencyRepository;
        this.tokensService = tokensService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass)
    {
        return (T) new SetPriceAlertViewModel(
                currencyRepository,
                tokensService
        );
    }
}
