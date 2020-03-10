package com.alphawallet.app.di;

import com.alphawallet.app.repository.LocaleRepository;
import com.alphawallet.app.repository.LocaleRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.viewmodel.AdvancedSettingsViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class AdvancedSettingsModule {
    @Provides
    AdvancedSettingsViewModelFactory provideAdvancedSettingsViewModelFactory(
            LocaleRepositoryType localeRepository
    ) {
        return new AdvancedSettingsViewModelFactory(
                localeRepository);
    }

    @Provides
    LocaleRepositoryType provideLocaleRepository(PreferenceRepositoryType preferenceRepository) {
        return new LocaleRepository(preferenceRepository);
    }
}
