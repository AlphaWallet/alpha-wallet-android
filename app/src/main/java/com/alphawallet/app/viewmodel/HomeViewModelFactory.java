package com.alphawallet.app.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.alphawallet.app.repository.LocaleRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.interact.FetchWalletsInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.router.AddTokenRouter;
import com.alphawallet.app.router.ExternalBrowserRouter;
import com.alphawallet.app.router.ImportTokenRouter;
import com.alphawallet.app.service.AssetDefinitionService;

public class HomeViewModelFactory implements ViewModelProvider.Factory {
    private final PreferenceRepositoryType preferenceRepository;
    private final ExternalBrowserRouter externalBrowserRouter;
    private final ImportTokenRouter importTokenRouter;
    private final AddTokenRouter addTokenRouter;
    private final LocaleRepositoryType localeRepository;
    private final AssetDefinitionService assetDefinitionService;
    private final GenericWalletInteract genericWalletInteract;
    private final FetchWalletsInteract fetchWalletsInteract;

    public HomeViewModelFactory(
            PreferenceRepositoryType preferenceRepository,
            LocaleRepositoryType localeRepository,
            ImportTokenRouter importTokenRouter,
            ExternalBrowserRouter externalBrowserRouter,
            AddTokenRouter addTokenRouter,
            AssetDefinitionService assetDefinitionService,
            GenericWalletInteract genericWalletInteract,
            FetchWalletsInteract fetchWalletsInteract) {
        this.preferenceRepository = preferenceRepository;
        this.localeRepository = localeRepository;
        this.importTokenRouter = importTokenRouter;
        this.externalBrowserRouter = externalBrowserRouter;
        this.addTokenRouter = addTokenRouter;
        this.assetDefinitionService = assetDefinitionService;
        this.genericWalletInteract = genericWalletInteract;
        this.fetchWalletsInteract = fetchWalletsInteract;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new HomeViewModel(
                preferenceRepository,
                localeRepository,
                importTokenRouter,
                externalBrowserRouter,
                addTokenRouter,
                assetDefinitionService,
                genericWalletInteract,
                fetchWalletsInteract
                );
    }
}
