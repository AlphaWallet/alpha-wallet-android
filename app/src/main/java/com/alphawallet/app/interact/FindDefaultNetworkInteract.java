package com.alphawallet.app.interact;

import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Ticker;

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

    //get the ticker
    public Single<Ticker> getTicker(int chainId) {
        return ethereumNetworkRepository.getTicker(chainId, null);
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
