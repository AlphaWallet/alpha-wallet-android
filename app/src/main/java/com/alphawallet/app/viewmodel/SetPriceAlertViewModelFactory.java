package com.alphawallet.app.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.repository.CurrencyRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;

public class SetPriceAlertViewModelFactory implements ViewModelProvider.Factory {
    private final CurrencyRepositoryType currencyRepository;
    private final PreferenceRepositoryType preferenceRepository;

    public SetPriceAlertViewModelFactory(
            CurrencyRepositoryType currencyRepository,
            PreferenceRepositoryType preferenceRepository)
    {
        this.currencyRepository = currencyRepository;
        this.preferenceRepository = preferenceRepository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass)
    {
        return (T) new SetPriceAlertViewModel(
                currencyRepository,
                preferenceRepository
        );
    }
}
