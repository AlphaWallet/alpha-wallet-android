package io.stormbird.wallet.interact;

import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Ticker;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;

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
