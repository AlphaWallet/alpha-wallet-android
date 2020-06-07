package com.alphawallet.app.viewmodel;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.repository.EthereumNetworkRepositoryType;

import io.reactivex.annotations.NonNull;

import com.alphawallet.app.service.TokensService;

public class SelectNetworkViewModelFactory implements ViewModelProvider.Factory {

    private final EthereumNetworkRepositoryType networkRepository;
    private final TokensService tokensService;

    public SelectNetworkViewModelFactory(EthereumNetworkRepositoryType networkRepository,
                                         TokensService tokensService) {
        this.networkRepository = networkRepository;
        this.tokensService = tokensService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new SelectNetworkViewModel(networkRepository, tokensService);
    }
}
