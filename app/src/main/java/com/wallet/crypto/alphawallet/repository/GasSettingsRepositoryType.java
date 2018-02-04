package com.wallet.crypto.alphawallet.repository;


import com.wallet.crypto.alphawallet.entity.GasSettings;

import io.reactivex.Single;

public interface GasSettingsRepositoryType {
    public Single<GasSettings> getGasSettings(boolean forTokenTransfer);
}
