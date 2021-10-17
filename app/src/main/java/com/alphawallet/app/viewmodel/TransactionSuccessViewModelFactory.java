package com.alphawallet.app.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.repository.PreferenceRepositoryType;

public class TransactionSuccessViewModelFactory implements ViewModelProvider.Factory
{
    private final PreferenceRepositoryType preferenceRepository;

    public TransactionSuccessViewModelFactory(
            PreferenceRepositoryType preferenceRepository)
    {
        this.preferenceRepository = preferenceRepository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass)
    {
        return (T) new TransactionSuccessViewModel(
                preferenceRepository);
    }
}
