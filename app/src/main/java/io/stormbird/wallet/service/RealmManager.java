package io.stormbird.wallet.service;

import io.stormbird.wallet.BuildConfig;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Wallet;

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

    public Realm getERC721RealmInstance(NetworkInfo network, Wallet wallet) {
        String name = get721Name(network, wallet);
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

    public Realm getWalletDataRealmInstance() {
        String name = "WalletData-db.realm";
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

    private String get721Name(NetworkInfo network, Wallet wallet) {
        return wallet.address + "-" + network.chainId + "-721-db.realm";
    }
}
