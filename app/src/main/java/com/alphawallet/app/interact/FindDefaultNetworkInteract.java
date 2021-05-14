package com.alphawallet.app.interact;

import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.entity.NetworkInfo;

public class FindDefaultNetworkInteract {

    private final EthereumNetworkRepositoryType ethereumNetworkRepository;

    public FindDefaultNetworkInteract(EthereumNetworkRepositoryType ethereumNetworkRepository) {
        this.ethereumNetworkRepository = ethereumNetworkRepository;
    }

    public String getNetworkName(int chainId)
    {
        return ethereumNetworkRepository.getNetworkByChain(chainId).getShortName();
    }

    public NetworkInfo getNetworkInfo(int chainId)
    {
        return ethereumNetworkRepository.getNetworkByChain(chainId);
    }
}
