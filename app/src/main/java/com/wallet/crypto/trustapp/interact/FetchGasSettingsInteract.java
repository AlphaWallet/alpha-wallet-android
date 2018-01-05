package com.wallet.crypto.trustapp.interact;


import com.wallet.crypto.trustapp.entity.GasSettings;
import com.wallet.crypto.trustapp.repository.PreferenceRepositoryType;

public class FetchGasSettingsInteract {
    private final PreferenceRepositoryType repository;

    public FetchGasSettingsInteract(PreferenceRepositoryType repository) {
        this.repository = repository;
    }

    public GasSettings fetch(boolean forTokenTransfer) {
        return repository.getGasSettings(forTokenTransfer);
    }

}
