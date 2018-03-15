package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.wallet.crypto.alphawallet.interact.AddTokenInteract;
import com.wallet.crypto.alphawallet.interact.FetchWalletsInteract;
import com.wallet.crypto.alphawallet.interact.ImportWalletInteract;
import com.wallet.crypto.alphawallet.repository.EthereumNetworkRepositoryType;

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
