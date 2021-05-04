package com.alphawallet.app.interact;

import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.entity.NetworkInfo;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class FindDefaultNetworkInteract {

    private final EthereumNetworkRepositoryType ethereumNetworkRepository;

    public FindDefaultNetworkInteract(EthereumNetworkRepositoryType ethereumNetworkRepository) {
        this.ethereumNetworkRepository = ethereumNetworkRepository;
    }

    public Single<NetworkInfo> find() {
        return Single.just(ethereumNetworkRepository.getDefaultNetwork())
                .observeOn(AndroidSchedulers.mainThread());
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
