package com.wallet.crypto.alphawallet.repository;


import com.wallet.crypto.alphawallet.C;
import com.wallet.crypto.alphawallet.entity.GasSettings;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.http.HttpService;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;

public class GasSettingsRepository implements GasSettingsRepositoryType {

    private final EthereumNetworkRepositoryType networkRepository;
    private BigInteger cachedGasPrice;
    private Disposable gasSettingsDisposable;

    private final static long FETCH_GAS_PRICE_INTERVAL = 60;

    public GasSettingsRepository(EthereumNetworkRepositoryType networkRepository) {
        this.networkRepository = networkRepository;

        cachedGasPrice = new BigInteger(C.DEFAULT_GAS_PRICE);
        gasSettingsDisposable = Observable.interval(0, FETCH_GAS_PRICE_INTERVAL, TimeUnit.SECONDS)
                .doOnNext(l ->
                        fetchGasSettings()
                ).subscribe();
    }

    private void fetchGasSettings() {

        final Web3j web3j = Web3jFactory.build(new HttpService(networkRepository.getDefaultNetwork().rpcServerUrl));

        try {
            EthGasPrice price = web3j
                    .ethGasPrice()
                    .send();
            cachedGasPrice = price.getGasPrice();
        } catch (Exception ex) {
            // silently
        }
    }

    public Single<GasSettings> getGasSettings(boolean forTokenTransfer) {
        return Single.fromCallable( () -> {
            BigInteger gasLimit = new BigInteger(C.DEFAULT_GAS_LIMIT);
            if (forTokenTransfer) {
                gasLimit = new BigInteger(C.DEFAULT_GAS_LIMIT_FOR_TOKENS);
            }
            return new GasSettings(cachedGasPrice, gasLimit);
        });
    }
}
