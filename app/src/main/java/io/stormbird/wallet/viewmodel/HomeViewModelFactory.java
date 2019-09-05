package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import io.stormbird.wallet.interact.FetchWalletsInteract;
import io.stormbird.wallet.interact.GenericWalletInteract;
import io.stormbird.wallet.repository.LocaleRepositoryType;
import io.stormbird.wallet.repository.PreferenceRepositoryType;
import io.stormbird.wallet.router.AddTokenRouter;
import io.stormbird.wallet.router.ExternalBrowserRouter;
import io.stormbird.wallet.router.ImportTokenRouter;
import io.stormbird.wallet.service.AssetDefinitionService;

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
