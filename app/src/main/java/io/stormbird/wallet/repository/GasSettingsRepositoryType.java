package io.stormbird.wallet.repository;


import io.stormbird.wallet.entity.GasSettings;

import io.reactivex.Single;

public interface GasSettingsRepositoryType {
    Single<GasSettings> getGasSettings(boolean forTokenTransfer);
    Single<GasSettings> getGasSettings(byte[] transactionBytes);
}
