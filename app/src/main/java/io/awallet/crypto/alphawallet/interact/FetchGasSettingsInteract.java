package io.awallet.crypto.alphawallet.interact;


import io.awallet.crypto.alphawallet.C;
import io.awallet.crypto.alphawallet.entity.GasSettings;
import io.awallet.crypto.alphawallet.repository.GasSettingsRepositoryType;

import java.math.BigInteger;

import io.reactivex.Single;

public class FetchGasSettingsInteract {
    private final GasSettingsRepositoryType repository;

    public FetchGasSettingsInteract(GasSettingsRepositoryType repository) {
        this.repository = repository;
    }

    public Single<GasSettings> fetch(boolean forTokenTransfer) {
        return repository.getGasSettings(forTokenTransfer);
    }

    public Single<GasSettings> fetchDefault(boolean tokenTransfer) {
        return Single.fromCallable(() -> {
            BigInteger gasPrice = new BigInteger(C.DEFAULT_GAS_PRICE);
            BigInteger gasLimit = new BigInteger(C.DEFAULT_GAS_LIMIT);
            if (tokenTransfer) {
                gasLimit = new BigInteger(C.DEFAULT_GAS_LIMIT_FOR_TOKENS);
            }
            return new GasSettings(gasPrice, gasLimit);
        });
    }
}
