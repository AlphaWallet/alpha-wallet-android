package io.awallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import io.awallet.crypto.alphawallet.interact.AddTokenInteract;
import io.awallet.crypto.alphawallet.interact.CreateWalletInteract;
import io.awallet.crypto.alphawallet.interact.FetchWalletsInteract;
import io.awallet.crypto.alphawallet.interact.ImportWalletInteract;
import io.awallet.crypto.alphawallet.repository.EthereumNetworkRepositoryType;

public class SplashViewModelFactory implements ViewModelProvider.Factory {

    private final FetchWalletsInteract fetchWalletsInteract;
    private final EthereumNetworkRepositoryType networkRepository;
    private final ImportWalletInteract importWalletInteract;
    private final AddTokenInteract addTokenInteract;
    private final CreateWalletInteract createWalletInteract;

    public SplashViewModelFactory(FetchWalletsInteract fetchWalletsInteract,
                                  EthereumNetworkRepositoryType networkRepository,
                                  ImportWalletInteract importWalletInteract,
                                  AddTokenInteract addTokenInteract,
                                  CreateWalletInteract createWalletInteract) {
        this.fetchWalletsInteract = fetchWalletsInteract;
        this.networkRepository = networkRepository;
        this.importWalletInteract = importWalletInteract;
        this.addTokenInteract = addTokenInteract;
        this.createWalletInteract = createWalletInteract;
    }
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new SplashViewModel(fetchWalletsInteract, networkRepository, importWalletInteract, addTokenInteract, createWalletInteract);
    }
}
