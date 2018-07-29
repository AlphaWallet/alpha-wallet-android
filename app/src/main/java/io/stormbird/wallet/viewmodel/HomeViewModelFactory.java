package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import io.stormbird.wallet.repository.LocaleRepositoryType;
import io.stormbird.wallet.router.AddTokenRouter;
import io.stormbird.wallet.router.ExternalBrowserRouter;
import io.stormbird.wallet.router.ImportTokenRouter;
import io.stormbird.wallet.router.SettingsRouter;
import io.stormbird.wallet.service.AssetDefinitionService;

public class HomeViewModelFactory implements ViewModelProvider.Factory {

    private final SettingsRouter settingsRouter;
    private final ExternalBrowserRouter externalBrowserRouter;
    private final ImportTokenRouter importTokenRouter;
    private final AddTokenRouter addTokenRouter;
    private final LocaleRepositoryType localeRepository;
    private final AssetDefinitionService assetDefinitionService;

    public HomeViewModelFactory(
            LocaleRepositoryType localeRepository,
            ImportTokenRouter importTokenRouter,
            ExternalBrowserRouter externalBrowserRouter,
            AddTokenRouter addTokenRouter,
            SettingsRouter settingsRouter,
            AssetDefinitionService assetDefinitionService) {
        this.localeRepository = localeRepository;
        this.importTokenRouter = importTokenRouter;
        this.externalBrowserRouter = externalBrowserRouter;
        this.addTokenRouter = addTokenRouter;
        this.settingsRouter = settingsRouter;
        this.assetDefinitionService = assetDefinitionService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new HomeViewModel(
                localeRepository,
        importTokenRouter,
        externalBrowserRouter,
        addTokenRouter,
        settingsRouter,
        assetDefinitionService);
    }
}
