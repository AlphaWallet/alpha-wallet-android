package com.alphawallet.app.viewmodel;


import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.alphawallet.app.repository.LocaleRepositoryType;

public class AdvancedSettingsViewModelFactory implements ViewModelProvider.Factory {
    private final LocaleRepositoryType localeRepository;

    public AdvancedSettingsViewModelFactory(
            LocaleRepositoryType localeRepository) {
        this.localeRepository = localeRepository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new AdvancedSettingsViewModel(
                localeRepository
        );
    }
}
