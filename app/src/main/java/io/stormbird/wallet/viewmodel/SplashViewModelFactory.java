package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import io.stormbird.wallet.interact.AddTokenInteract;
import io.stormbird.wallet.interact.CreateWalletInteract;
import io.stormbird.wallet.interact.FetchWalletsInteract;
import io.stormbird.wallet.interact.ImportWalletInteract;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.repository.LocaleRepositoryType;
import io.stormbird.wallet.repository.PreferenceRepositoryType;

public class SplashViewModelFactory implements ViewModelProvider.Factory {

    private final FetchWalletsInteract fetchWalletsInteract;
    private final EthereumNetworkRepositoryType networkRepository;
    private final ImportWalletInteract importWalletInteract;
    private final AddTokenInteract addTokenInteract;
    private final CreateWalletInteract createWalletInteract;
    private final PreferenceRepositoryType preferenceRepository;
    private final LocaleRepositoryType localeRepository;

    public SplashViewModelFactory(FetchWalletsInteract fetchWalletsInteract,
                                  EthereumNetworkRepositoryType networkRepository,
                                  ImportWalletInteract importWalletInteract,
                                  AddTokenInteract addTokenInteract,
                                  CreateWalletInteract createWalletInteract,
                                  PreferenceRepositoryType preferenceRepository,
                                  LocaleRepositoryType localeRepository) {
        this.fetchWalletsInteract = fetchWalletsInteract;
        this.networkRepository = networkRepository;
        this.importWalletInteract = importWalletInteract;
        this.addTokenInteract = addTokenInteract;
        this.createWalletInteract = createWalletInteract;
        this.preferenceRepository = preferenceRepository;
        this.localeRepository = localeRepository;
    }
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new SplashViewModel(fetchWalletsInteract, networkRepository, importWalletInteract, addTokenInteract, createWalletInteract, preferenceRepository, localeRepository);
    }
}
