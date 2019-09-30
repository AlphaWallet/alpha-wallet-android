package com.alphawallet.app.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.alphawallet.app.repository.LocaleRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.interact.FetchWalletsInteract;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.KeyService;

public class SplashViewModelFactory implements ViewModelProvider.Factory {

    private final FetchWalletsInteract fetchWalletsInteract;
    private final PreferenceRepositoryType preferenceRepository;
    private final LocaleRepositoryType localeRepository;
    private final KeyService keyService;
    private final AssetDefinitionService assetDefinitionService;

    public SplashViewModelFactory(FetchWalletsInteract fetchWalletsInteract,
                                  PreferenceRepositoryType preferenceRepository,
                                  LocaleRepositoryType localeRepository,
                                  KeyService keyService,
                                  AssetDefinitionService assetDefinitionService) {
        this.fetchWalletsInteract = fetchWalletsInteract;
        this.preferenceRepository = preferenceRepository;
        this.localeRepository = localeRepository;
        this.keyService = keyService;
        this.assetDefinitionService = assetDefinitionService;
    }
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new SplashViewModel(fetchWalletsInteract, preferenceRepository, localeRepository, keyService, assetDefinitionService);
    }
}
