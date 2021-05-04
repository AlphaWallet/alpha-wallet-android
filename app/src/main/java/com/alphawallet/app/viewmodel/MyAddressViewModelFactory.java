package com.alphawallet.app.viewmodel;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.service.TokensService;

public class MyAddressViewModelFactory implements ViewModelProvider.Factory {
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final TokensService tokensService;

    public MyAddressViewModelFactory(
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            TokensService tokensService) {
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.tokensService = tokensService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new MyAddressViewModel(
                ethereumNetworkRepository,
                tokensService
                );
    }
}
