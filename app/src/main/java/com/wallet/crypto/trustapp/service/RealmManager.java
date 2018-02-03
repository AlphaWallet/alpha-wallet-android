package com.wallet.crypto.trustapp.service;

import com.wallet.crypto.trustapp.BuildConfig;
import com.wallet.crypto.trustapp.entity.NetworkInfo;
import com.wallet.crypto.trustapp.entity.Wallet;

import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class RealmManager {

    private final Map<String, RealmConfiguration> realmConfigurations = new HashMap<>();

    public Realm getRealmInstance(NetworkInfo networkInfo, Wallet wallet) {
        String name = getName(networkInfo, wallet);
        RealmConfiguration config = realmConfigurations.get(name);
        if (config == null) {
            config = new RealmConfiguration.Builder()
                    .name(name)
                    .schemaVersion(BuildConfig.DB_VERSION)
                    .deleteRealmIfMigrationNeeded()
                    .build();
            realmConfigurations.put(name, config);
        }
        return Realm.getInstance(config);
    }

    private String getName(NetworkInfo networkInfo, Wallet wallet) {
        return wallet.address + "-" + networkInfo.name + "-db.realm";
    }
}
