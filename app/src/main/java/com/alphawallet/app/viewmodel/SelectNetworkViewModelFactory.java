package com.alphawallet.app.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;

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
