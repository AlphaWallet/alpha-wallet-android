package io.stormbird.wallet.repository;


import android.arch.lifecycle.MutableLiveData;
import io.stormbird.wallet.entity.GasSettings;

import io.reactivex.Single;

import java.math.BigInteger;

public interface GasSettingsRepositoryType {
    Single<GasSettings> getGasSettings(boolean forTokenTransfer);
    Single<GasSettings> getGasSettings(byte[] transactionBytes, boolean isNonFungible, int chainId);
    MutableLiveData<BigInteger> gasPriceUpdate();
    void startGasListener(int chainId);
    void stopGasListener();
}
