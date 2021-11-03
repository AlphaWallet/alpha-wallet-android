package com.alphawallet.app.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.interact.FetchWalletsInteract;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.service.KeyService;

public class SplashViewModelFactory implements ViewModelProvider.Factory {

    private final FetchWalletsInteract fetchWalletsInteract;
    private final PreferenceRepositoryType preferenceRepository;
    private final KeyService keyService;

    public SplashViewModelFactory(FetchWalletsInteract fetchWalletsInteract,
                                  PreferenceRepositoryType preferenceRepository,
                                  KeyService keyService) {
        this.fetchWalletsInteract = fetchWalletsInteract;
        this.preferenceRepository = preferenceRepository;
        this.keyService = keyService;
    }
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new SplashViewModel(
                fetchWalletsInteract,
                preferenceRepository,
                keyService);
    }
}
