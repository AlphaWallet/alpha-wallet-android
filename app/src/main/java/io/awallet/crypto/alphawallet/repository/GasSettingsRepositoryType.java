package io.awallet.crypto.alphawallet.repository;


import io.awallet.crypto.alphawallet.entity.GasSettings;

import io.reactivex.Single;

public interface GasSettingsRepositoryType {
    public Single<GasSettings> getGasSettings(boolean forTokenTransfer);
}
