package io.stormbird.wallet.repository;


import io.stormbird.wallet.entity.GasSettings;

import io.reactivex.Single;

public interface GasSettingsRepositoryType {
    public Single<GasSettings> getGasSettings(boolean forTokenTransfer);
}
