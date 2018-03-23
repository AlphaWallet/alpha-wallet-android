package io.awallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import io.awallet.crypto.alphawallet.interact.AddTokenInteract;
import io.awallet.crypto.alphawallet.interact.FetchWalletsInteract;
import io.awallet.crypto.alphawallet.interact.ImportWalletInteract;
import io.awallet.crypto.alphawallet.repository.EthereumNetworkRepositoryType;

public class SplashViewModelFactory implements ViewModelProvider.Factory {

    private final FetchWalletsInteract fetchWalletsInteract;
    private final EthereumNetworkRepositoryType networkRepository;
    private final ImportWalletInteract importWalletInteract;
    private final AddTokenInteract addTokenInteract;

    public SplashViewModelFactory(FetchWalletsInteract fetchWalletsInteract,
                                  EthereumNetworkRepositoryType networkRepository,
                                  ImportWalletInteract importWalletInteract,
                                  AddTokenInteract addTokenInteract) {
        this.fetchWalletsInteract = fetchWalletsInteract;
        this.networkRepository = networkRepository;
        this.importWalletInteract = importWalletInteract;
        this.addTokenInteract = addTokenInteract;
    }
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new SplashViewModel(fetchWalletsInteract, networkRepository, importWalletInteract, addTokenInteract);
    }
}
