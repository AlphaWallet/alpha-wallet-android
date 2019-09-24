package com.alphawallet.app.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;

public class MyAddressViewModelFactory implements ViewModelProvider.Factory {
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final TokenRepositoryType tokenRepository;

    public MyAddressViewModelFactory(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            TokenRepositoryType tokenRepository) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.tokenRepository = tokenRepository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new MyAddressViewModel(
                findDefaultNetworkInteract,
                ethereumNetworkRepository,
                tokenRepository
                );
    }
}
