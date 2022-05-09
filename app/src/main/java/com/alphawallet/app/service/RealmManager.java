package com.alphawallet.app.service;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.repository.AWRealmMigration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class RealmManager {

    private final Map<String, RealmConfiguration> realmConfigurations = new HashMap<>();

    public String getRealmInstanceName(Wallet wallet) {
        return wallet.address.toLowerCase() + "-db.realm";
    }

    public Realm getRealmInstance(Wallet wallet) {
        return getRealmInstanceInternal(getRealmInstanceName(wallet));
    }

    public Realm getRealmInstance(String walletAddress) {
        return getRealmInstanceInternal(walletAddress.toLowerCase() + "-db.realm");
    }

    Realm getRealmInstanceInternal(String name) {
//        try
//        {
            RealmConfiguration config = realmConfigurations.get(name);
            if (config == null)
            {
                config = new RealmConfiguration.Builder(Collections.emptySet()).name(name)
                        .schemaVersion(BuildConfig.DB_VERSION)
                        .migration(new AWRealmMigration())
                        .build();
                realmConfigurations.put(name, config);
            }
            return Realm.Companion.open(config);
//        }
//        catch ( e)
//        {
//            //we require a realm migration, but this wasn't provided.
//            RealmConfiguration config = realmConfigurations.get(name);
//            if (config == null)
//            {
//                config = new RealmConfiguration.Builder().name(name)
//                        .schemaVersion(BuildConfig.DB_VERSION)
//                        .deleteRealmIfMigrationNeeded()
//                        .build();
//                realmConfigurations.put(name, config);
//            }
//            return Realm.Companion.open(config);
//        }
    }

    public Realm getWalletDataRealmInstance() {
        return getRealmInstanceInternal("WalletData-db.realm");
    }

    public Realm getWalletTypeRealmInstance() {
        return getRealmInstanceInternal("WalletType-db.realm");
    }
}
