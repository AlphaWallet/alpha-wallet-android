package io.stormbird.wallet.di;

import dagger.Module;
import dagger.Provides;
import io.stormbird.wallet.repository.LocaleRepository;
import io.stormbird.wallet.repository.LocaleRepositoryType;
import io.stormbird.wallet.repository.PreferenceRepositoryType;
import io.stormbird.wallet.router.AddTokenRouter;
import io.stormbird.wallet.router.ExternalBrowserRouter;
import io.stormbird.wallet.router.ImportTokenRouter;
import io.stormbird.wallet.router.SettingsRouter;
import io.stormbird.wallet.viewmodel.HomeViewModelFactory;

@Module
class HomeModule {
    @Provides
    HomeViewModelFactory provideTransactionsViewModelFactory(
            LocaleRepositoryType localeRepository,
            ImportTokenRouter importTokenRouter,
            ExternalBrowserRouter externalBrowserRouter,
            AddTokenRouter addTokenRouter,
            SettingsRouter settingsRouter) {
        return new HomeViewModelFactory(
                localeRepository,
                importTokenRouter,
                externalBrowserRouter,
                addTokenRouter,
                settingsRouter);
    }

    @Provides
    LocaleRepositoryType provideLocaleRepository(PreferenceRepositoryType preferenceRepository) {
        return new LocaleRepository(preferenceRepository);
    }

    @Provides
    SettingsRouter provideSettingsRouter() {
        return new SettingsRouter();
    }

    @Provides
    AddTokenRouter provideAddTokenRouter() {
        return new AddTokenRouter();
    }

    @Provides
    ExternalBrowserRouter provideExternalBrowserRouter() {
        return new ExternalBrowserRouter();
    }

    @Provides
    ImportTokenRouter providesImportTokenRouter() { return new ImportTokenRouter(); }

}
