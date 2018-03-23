package io.awallet.crypto.alphawallet.interact;

import io.awallet.crypto.alphawallet.entity.NetworkInfo;
import io.awallet.crypto.alphawallet.entity.Ticker;
import io.awallet.crypto.alphawallet.repository.EthereumNetworkRepositoryType;

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
    public Single<Ticker> getTicker() {
        return ethereumNetworkRepository.getTicker();
    }
}
