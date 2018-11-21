package io.stormbird.wallet.repository;


import io.stormbird.wallet.C;
import io.stormbird.wallet.entity.GasSettings;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.http.HttpService;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;

import static io.stormbird.wallet.C.GAS_LIMIT_MIN;
import static io.stormbird.wallet.C.GAS_PER_BYTE;

public class GasSettingsRepository implements GasSettingsRepositoryType
{
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

        final Web3j web3j = Web3jFactory.build(new HttpService(networkRepository.getActiveRPC()));

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

    public Single<GasSettings> getGasSettings(byte[] transactionBytes) {
        return Single.fromCallable( () -> {
            BigInteger gasLimit = new BigInteger(C.DEFAULT_GAS_LIMIT);
            if (transactionBytes != null) {
                gasLimit = new BigInteger(C.DEFAULT_GAS_LIMIT_FOR_TOKENS);
                BigInteger estimate = estimateGasLimit(transactionBytes);
                if (estimate.compareTo(gasLimit) > 0) gasLimit = estimate;
            }
            return new GasSettings(cachedGasPrice, gasLimit);
        });
    }

    private BigInteger estimateGasLimit(byte[] data)
    {
        BigInteger roundingFactor = BigInteger.valueOf(10000);
        BigInteger txMin = BigInteger.valueOf(GAS_LIMIT_MIN);
        BigInteger bytePrice = BigInteger.valueOf(GAS_PER_BYTE);
        BigInteger dataLength = BigInteger.valueOf(data.length);
        BigInteger estimate = bytePrice.multiply(dataLength).add(txMin);
        estimate = estimate.divide(roundingFactor).add(BigInteger.ONE).multiply(roundingFactor);
        return estimate;
    }
}
