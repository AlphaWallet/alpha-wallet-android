package com.langitwallet.app.viewmodel;

import com.langitwallet.app.entity.NetworkInfo;
import com.langitwallet.app.repository.EthereumNetworkRepositoryType;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class NodeStatusViewModel extends BaseViewModel {

    private final EthereumNetworkRepositoryType networkRepository;

    @Inject
    public NodeStatusViewModel(EthereumNetworkRepositoryType ethereumNetworkRepositoryType)
    {
        this.networkRepository = ethereumNetworkRepositoryType;
    }

    public NetworkInfo[] getNetworkList()
    {
        return networkRepository.getAvailableNetworkList();
    }
}
