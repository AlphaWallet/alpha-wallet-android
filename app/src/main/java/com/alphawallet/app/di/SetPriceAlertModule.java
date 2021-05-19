package com.alphawallet.app.di;

import com.alphawallet.app.repository.CurrencyRepository;
import com.alphawallet.app.repository.CurrencyRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.viewmodel.SetPriceAlertViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class SetPriceAlertModule {
    @Provides
    SetPriceAlertViewModelFactory provideSetPriceAlertViewModelFactory(
            CurrencyRepositoryType currencyRepository,
            PreferenceRepositoryType preferenceRepository
    )
    {
        return new SetPriceAlertViewModelFactory(
                currencyRepository,
                preferenceRepository);
    }

    @Provides
    CurrencyRepositoryType provideCurrencyRepository(PreferenceRepositoryType preferenceRepository)
    {
        return new CurrencyRepository(preferenceRepository);
    }
}
