package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;
import io.stormbird.wallet.interact.CreateWalletInteract;
import io.stormbird.wallet.interact.FetchWalletsInteract;
import io.stormbird.wallet.repository.LocaleRepositoryType;
import io.stormbird.wallet.repository.PreferenceRepositoryType;

public class SplashViewModelFactory implements ViewModelProvider.Factory {

    private final FetchWalletsInteract fetchWalletsInteract;
    private final CreateWalletInteract createWalletInteract;
    private final PreferenceRepositoryType preferenceRepository;
    private final LocaleRepositoryType localeRepository;

    public SplashViewModelFactory(FetchWalletsInteract fetchWalletsInteract,
                                  CreateWalletInteract createWalletInteract,
                                  PreferenceRepositoryType preferenceRepository,
                                  LocaleRepositoryType localeRepository) {
        this.fetchWalletsInteract = fetchWalletsInteract;
        this.createWalletInteract = createWalletInteract;
        this.preferenceRepository = preferenceRepository;
        this.localeRepository = localeRepository;
    }
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new SplashViewModel(fetchWalletsInteract, createWalletInteract, preferenceRepository, localeRepository);
    }
}
