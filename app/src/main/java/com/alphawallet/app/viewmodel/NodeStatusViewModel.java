package com.alphawallet.app.viewmodel;

import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;

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
