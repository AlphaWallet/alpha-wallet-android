package io.stormbird.wallet.interact;


import android.arch.lifecycle.MutableLiveData;
import io.stormbird.wallet.C;
import io.stormbird.wallet.entity.GasSettings;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.repository.GasSettingsRepositoryType;

import java.math.BigInteger;

import io.reactivex.Single;

import static io.stormbird.wallet.repository.EthereumNetworkRepository.XDAI_ID;

public class FetchGasSettingsInteract {
    private final GasSettingsRepositoryType repository;

    public FetchGasSettingsInteract(GasSettingsRepositoryType repository) {
        this.repository = repository;
    }

    public void startGasSettingsFetch(int chainId)
    {
        repository.startGasListener(chainId);
    }

    public void stopGasSettingsFetch()
    {
        repository.stopGasListener();
    }

    public Single<GasSettings> fetch(boolean forTokenTransfer) {
        return repository.getGasSettings(forTokenTransfer);
    }

    public Single<GasSettings> fetch(byte[] transactionBytes, boolean isNonFungible, int chainId) {
        return repository.getGasSettings(transactionBytes, isNonFungible, chainId);
    }

    public MutableLiveData<BigInteger> gasPriceUpdate()
    {
        return repository.gasPriceUpdate();
    }

    public Single<GasSettings> fetchDefault(boolean tokenTransfer, NetworkInfo networkInfo) {
        return Single.fromCallable(() -> {
            BigInteger gasPrice = new BigInteger(C.DEFAULT_GAS_PRICE);
            if (networkInfo.chainId == XDAI_ID)
            {
                gasPrice = new BigInteger(C.DEFAULT_XDAI_GAS_PRICE);
            }

            BigInteger gasLimit = new BigInteger(C.DEFAULT_GAS_LIMIT);
            if (tokenTransfer) {
                gasLimit = new BigInteger(C.DEFAULT_GAS_LIMIT_FOR_TOKENS);
            }
            return new GasSettings(gasPrice, gasLimit);
        });
    }
}
