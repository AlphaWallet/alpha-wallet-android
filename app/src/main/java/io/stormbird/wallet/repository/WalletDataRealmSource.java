package io.stormbird.wallet.repository;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Single;
import io.realm.Realm;
import io.realm.RealmResults;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.repository.entity.RealmToken;
import io.stormbird.wallet.repository.entity.RealmWalletData;
import io.stormbird.wallet.service.RealmManager;

/**
 * Created by James on 8/11/2018.
 * Stormbird in Singapore
 */

public class WalletDataRealmSource {
    private static final String TAG = WalletDataRealmSource.class.getSimpleName();

    private final RealmManager realmManager;

    public WalletDataRealmSource(RealmManager realmManager) {
        this.realmManager = realmManager;
    }

    public Single<Wallet[]> populateWalletData(Wallet[] wallets) {
        return Single.fromCallable(() -> {
            List<Wallet> walletList = new ArrayList<>();
            Map<String, Wallet> wMap = new HashMap<>();
            for (Wallet w : wallets) if (w.address != null) wMap.put(w.address, w);

            try (Realm realm = realmManager.getWalletDataRealmInstance()) {
                RealmResults<RealmWalletData> data = realm.where(RealmWalletData.class)
                        .findAll();

                for (RealmWalletData d : data)
                {
                    if (d != null)
                    {
                        Wallet wallet = new Wallet(d.getAddress());
                        if (wMap.containsKey(d.getAddress()))
                        {
                            wallet.setWalletType(Wallet.WalletType.KEYSTORE);
                        }
                        else
                        {
                            wallet.setWalletType(Wallet.WalletType.HDKEY);
                        }
                        wallet.ENSname = d.getENSName();
                        wallet.balance = balance(d);
                        wallet.name = d.getName();
                        walletList.add(wallet);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            //now add types from legacy store
            for (Wallet w : wallets)
            {
                boolean found = false;
                for (Wallet wl : walletList)
                {
                    if (wl.address.equals(w.address)) { found = true; break; }
                }
                if (!found)
                {
                    w.setWalletType(Wallet.WalletType.KEYSTORE);
                    walletList.add(w);
                }
            }

            return walletList.toArray(new Wallet[0]);
        });
    }

    private String balance(RealmWalletData data)
    {
        String value = data.getBalance();
        if (value == null) return "0";
        else return value;
    }

    public Single<Wallet[]> loadWallets() {
        return Single.fromCallable(() -> {
            List<Wallet> wallets = new ArrayList<>();
            try (Realm realm = realmManager.getWalletDataRealmInstance()) {
                RealmResults<RealmWalletData> realmItems = realm.where(RealmWalletData.class)
                        .findAll();

                for (RealmWalletData data : realmItems) {
                    Wallet thisWallet = convertWallet(data);
                    wallets.add(thisWallet);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return wallets.toArray(new Wallet[0]);
        });
    }

    private Wallet convertWallet(RealmWalletData data) {
        Wallet wallet = new Wallet(data.getAddress());
        wallet.ENSname = data.getENSName();
        wallet.balance = data.getBalance();
        wallet.name = data.getName();
        return wallet;
    }

    public Single<Integer> storeWallets(Wallet[] wallets, boolean mainNet) {
        return Single.fromCallable(() -> {
            Integer updated = 0;
            try (Realm realm = realmManager.getWalletDataRealmInstance()) {
                realm.beginTransaction();

                for (Wallet wallet : wallets) {
                    RealmWalletData realmWallet = realm.where(RealmWalletData.class)
                            .equalTo("address", wallet.address)
                            .findFirst();

                    if (realmWallet == null) {
                        realmWallet = realm.createObject(RealmWalletData.class, wallet.address);
                        realmWallet.setENSName(wallet.ENSname);
                        if (mainNet) realmWallet.setBalance(wallet.balance);
                        realmWallet.setName(wallet.name);
                        updated++;
                    } else {
                        if (mainNet && (realmWallet.getBalance() == null || !wallet.balance.equals(realmWallet.getENSName())))
                            realmWallet.setBalance(wallet.balance);
                        if (wallet.ENSname != null && (realmWallet.getENSName() == null || !wallet.ENSname.equals(realmWallet.getENSName())))
                            realmWallet.setENSName(wallet.ENSname);
                        realmWallet.setName(wallet.name);
                        updated++;
                    }
                }
                realm.commitTransaction();
            } catch (Exception e) {
                Log.e(TAG, "storeWallets: " + e.getMessage(), e);
            }
            return updated;
        });
    }

    public Single<Wallet> storeWallet(Wallet wallet) {
        return Single.fromCallable(() -> {
            try (Realm realm = realmManager.getWalletDataRealmInstance()) {
                realm.beginTransaction();

                RealmWalletData realmWallet = realm.where(RealmWalletData.class)
                        .equalTo("address", wallet.address)
                        .findFirst();

                if (realmWallet == null) {
                    realmWallet = realm.createObject(RealmWalletData.class, wallet.address);
                }

                realmWallet.setName(wallet.name);
                realmWallet.setType(wallet.type);
                realmWallet.setENSName(wallet.ENSname);
                realmWallet.setBalance(wallet.balance);
                realmWallet.setType(wallet.type);

                realm.commitTransaction();
            } catch (Exception e) {
                Log.e(TAG, "storeWallet: " + e.getMessage(), e);
            }
            return wallet;
        });
    }

    public Single<String> getName(String address) {
        return Single.fromCallable(() -> {
            String name = "";
            try (Realm realm = realmManager.getWalletDataRealmInstance()) {
                RealmWalletData realmWallet = realm.where(RealmWalletData.class)
                        .equalTo("address", address)
                        .findFirst();
                if (realmWallet != null) name = realmWallet.getName();
            } catch (Exception e) {
                Log.e(TAG, "getName: " + e.getMessage(), e);
            }
            return name;
        });
    }
}
