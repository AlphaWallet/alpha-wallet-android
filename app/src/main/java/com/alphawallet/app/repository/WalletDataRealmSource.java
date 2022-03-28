package com.alphawallet.app.repository;

import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.entity.tokenscript.TokenscriptFunction;
import com.alphawallet.app.repository.entity.RealmKeyType;
import com.alphawallet.app.repository.entity.RealmWalletData;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.RealmManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import timber.log.Timber;

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

    public Single<Wallet[]> populateWalletData(Wallet[] keystoreWallets, KeyService keyService)
    {
        return Single.fromCallable(() -> {
            Map<String, Wallet> walletList;
            try (Realm realm = realmManager.getWalletDataRealmInstance())
            {
                walletList = loadOrCreateKeyRealmDB(realm, keystoreWallets); //call has action on upgrade to new UX
                //Add additional - non critical wallet data. This database can be voided for upgrade if required
                for (Wallet wallet : walletList.values())
                {
                    RealmWalletData data = realm.where(RealmWalletData.class)
                            .equalTo("address", wallet.address, Case.INSENSITIVE)
                            .findFirst();

                    composeWallet(wallet, data);
                }

                recoverLostWallets(realm, keystoreWallets, walletList, keyService);
            }

            migrateWalletTypeData(walletList);

            Timber.tag("RealmDebug").d("populate %s", walletList.size());
            return walletList.values().toArray(new Wallet[0]);
        });
    }

    private Map<String, Wallet> loadOrCreateKeyRealmDB(Realm realm, Wallet[] wallets)
    {
        Map<String, Wallet> walletList = new HashMap<>();
        List<String> keyStoreList = walletArrayToAddressList(wallets);

        realm.refresh(); //ensure we're fully up to date before query
        RealmResults<RealmKeyType> realmKeyTypes = realm.where(RealmKeyType.class)
                .sort("dateAdded", Sort.ASCENDING)
                .findAll();

        if (realmKeyTypes.size() > 0)
        {
            //Load fixed wallet data: wallet type, creation and backup times
            for (RealmKeyType walletTypeData : realmKeyTypes)
            {
                Wallet w = composeKeyType(walletTypeData);
                if (w == null || (w.type == WalletType.KEYSTORE || w.type == WalletType.KEYSTORE_LEGACY) &&
                        !keyStoreList.contains(walletTypeData.getAddress().toLowerCase()))
                {
                    continue;
                }

                walletList.put(w.address.toLowerCase(), w);
            }
        }
        else //only zero on upgrade from v2.01.3 and lower (pre-HD key)
        {
            realm.executeTransaction(r -> {
                for (Wallet wallet : wallets)
                {
                    wallet.authLevel = KeyService.AuthenticationLevel.TEE_NO_AUTHENTICATION;
                    wallet.type = WalletType.KEYSTORE_LEGACY;
                    storeKeyData(wallet, r);
                    walletList.put(wallet.address.toLowerCase(), wallet);
                }
            });
        }

        Timber.tag("RealmDebug").d("loadorcreate " + walletList.size());
        return walletList;
    }

    private List<String> walletArrayToAddressList(Wallet[] wallets)
    {
        List<String> walletList = new ArrayList<>();
        for (Wallet w : wallets) { walletList.add(w.address.toLowerCase()); }
        return walletList;
    }

    private void composeWallet(Wallet wallet, RealmWalletData d)
    {
        if (d != null)
        {
            wallet.ENSname = d.getENSName();
            wallet.balance = balance(d);
            wallet.name = d.getName();
            wallet.ENSAvatar = d.getENSAvatar();
        }
    }

    private Wallet composeKeyType(RealmKeyType keyType)
    {
        Wallet wallet = null;
        if (keyType != null)
        {
            wallet = new Wallet(keyType.getAddress());
            wallet.type = keyType.getType();
            wallet.walletCreationTime = keyType.getDateAdded();
            wallet.lastBackupTime = keyType.getLastBackup();
            wallet.authLevel = keyType.getAuthLevel();
        }

        return wallet;
    }

    private String balance(RealmWalletData data)
    {
        String value = data.getBalance();
        if (value == null) return "0";
        else return value;
    }

    public Single<Wallet[]> storeWallets(Wallet[] wallets) {
        return Single.fromCallable(() -> {
            try (Realm realm = realmManager.getWalletDataRealmInstance()) {

                realm.executeTransaction(r -> {
                    for (Wallet wallet : wallets)
                    {
                        storeKeyData(wallet, r);
                    }
                });
            } catch (Exception e) {
                Timber.e(e, "storeWallets: %s", e.getMessage());
            }
            return wallets;
        });
    }

    public Single<Wallet> storeWallet(Wallet wallet) {
        return deleteWallet(wallet) //refresh data
        .flatMap(deletedWallet -> Single.fromCallable(() -> {
            storeKeyData(wallet);
            storeWalletData(wallet);
            return wallet;
        }));
    }

    public void updateWalletData(Wallet wallet, Realm.Transaction.OnSuccess onSuccess)
    {
        try (Realm realm = realmManager.getWalletDataRealmInstance())
        {
            realm.executeTransactionAsync(r -> {
                storeKeyData(wallet, r);
                Timber.tag("RealmDebug").d("storedwalletdata " + wallet.address);
            }, onSuccess);
        }
        catch (Exception e)
        {
            Timber.e(e);
            onSuccess.onSuccess();
        }
    }

    public Single<String> getName(String address) {
        return Single.fromCallable(() -> {
            String name = "";
            try (Realm realm = realmManager.getWalletDataRealmInstance()) {
                RealmWalletData realmWallet = realm.where(RealmWalletData.class)
                        .equalTo("address", address, Case.INSENSITIVE)
                        .findFirst();
                if (realmWallet != null) name = realmWallet.getName();
            } catch (Exception e) {
                Timber.e(e, "getName: %s", e.getMessage());
            }
            return name;
        });
    }

    public void updateBackupTime(String walletAddr)
    {
        updateWarningTime(walletAddr);

        try (Realm realm = realmManager.getWalletDataRealmInstance())
        {
            realm.executeTransactionAsync(r -> {
                RealmKeyType realmKey = r.where(RealmKeyType.class)
                        .equalTo("address", walletAddr, Case.INSENSITIVE)
                        .findFirst();

                if (realmKey != null)
                {
                    realmKey.setLastBackup(System.currentTimeMillis());
                }
            });
        }
    }

    public void updateWarningTime(String walletAddr)
    {
        try (Realm realm = realmManager.getWalletDataRealmInstance())
        {
            realm.executeTransactionAsync(r -> {
                RealmWalletData realmWallet = r.where(RealmWalletData.class)
                        .equalTo("address", walletAddr)
                        .findFirst();

                if (realmWallet != null)
                {
                    //Always update warning time but only update backup time if a backup was made
                    realmWallet.setLastWarning(System.currentTimeMillis());
                }
            });
        }
    }

    public Single<String> getWalletRequiresBackup(String walletAddr)
    {
        return Single.fromCallable(() -> {
            boolean wasDismissed = isDismissedInSettings(walletAddr);
            long backupTime = getKeyBackupTime(walletAddr);
            if (!wasDismissed && backupTime == 0) return walletAddr;
            else return "";
        });
    }

    public Single<Boolean> getWalletBackupWarning(String walletAddr)
    {
        return Single.fromCallable(() -> {
            long backupTime = getKeyBackupTime(walletAddr);
            long warningTime = getWalletWarningTime(walletAddr);
            return requiresBackup(backupTime, warningTime);
        });
    }

    public Single<Wallet> deleteWallet(Wallet wallet)
    {
        return Single.fromCallable(() -> {
            try (Realm realm = realmManager.getWalletDataRealmInstance())
            {
                realm.executeTransaction(r -> {
                    RealmResults<RealmWalletData> realmWallet = r.where(RealmWalletData.class).equalTo("address", wallet.address, Case.INSENSITIVE).findAll();
                    if (realmWallet != null) realmWallet.deleteAllFromRealm();
                    RealmResults<RealmKeyType> realmKey = r.where(RealmKeyType.class).equalTo("address", wallet.address, Case.INSENSITIVE).findAll();
                    if (realmKey != null) realmKey.deleteAllFromRealm();
                });
            }
            catch (Exception e)
            {
                Timber.e(e);
            }

            try (Realm instance = realmManager.getRealmInstance(wallet))
            {
                instance.executeTransaction(r -> r.deleteAll());
                instance.refresh();
            }
            catch (Exception e)
            {
                Timber.e(e);
            }

            return wallet;
        }).subscribeOn(Schedulers.io());
    }

    public void setIsDismissed(String walletAddr, boolean isDismissed)
    {
        try (Realm realm = realmManager.getWalletDataRealmInstance())
        {
            realm.executeTransactionAsync(r -> {
                RealmWalletData realmWallet = r.where(RealmWalletData.class)
                        .equalTo("address", walletAddr, Case.INSENSITIVE)
                        .findFirst();
                if (realmWallet != null)
                {
                    realmWallet.setIsDismissedInSettings(isDismissed);
                }
            });
        }
    }

    private void storeKeyData(Wallet wallet)
    {
        try (Realm realm = realmManager.getWalletDataRealmInstance())
        {
            realm.executeTransaction(r -> {
                storeKeyData(wallet, r);
                Timber.tag("RealmDebug").d("storedKeyData %s", wallet.address);
            });
        }
    }

    private void storeWalletData(Wallet wallet)
    {
        try (Realm realm = realmManager.getWalletDataRealmInstance())
        {
            realm.executeTransaction(r -> {
                storeWalletData(wallet, r);
                Timber.tag("RealmDebug").d("storedwalletdata %s", wallet.address);
            });
        }
        catch (Exception e)
        {
            Timber.e(e);
        }
    }

    private boolean isDismissedInSettings(String wallet)
    {
        try (Realm realm = realmManager.getWalletDataRealmInstance())
        {
            RealmWalletData data = realm.where(RealmWalletData.class)
                    .equalTo("address", wallet, Case.INSENSITIVE)
                    .findFirst();
            return data != null && data.getIsDismissedInSettings();
        }
    }

    private long getWalletWarningTime(String walletAddr)
    {
        try (Realm realm = realmManager.getWalletDataRealmInstance())
        {
            RealmWalletData data = realm.where(RealmWalletData.class)
                    .equalTo("address", walletAddr, Case.INSENSITIVE)
                    .findFirst();

            if (data != null)
            {
                return data.getLastWarning();
            }
            else
            {
                return 0;
            }
        }
    }

    private long getKeyBackupTime(String walletAddr)
    {
        try (Realm realm = realmManager.getWalletDataRealmInstance())
        {
            RealmKeyType realmKey = realm.where(RealmKeyType.class)
                    .equalTo("address", walletAddr, Case.INSENSITIVE)
                    .findFirst();

            if (realmKey != null)
            {
                return realmKey.getLastBackup();
            }
            else
            {
                return 0;
            }
        }
    }

    private Boolean requiresBackup(Long backupTime, Long warningTime)
    {
        boolean warningDismissed = false;
        if (System.currentTimeMillis() < (warningTime + KeyService.TIME_BETWEEN_BACKUP_WARNING_MILLIS))
        {
            warningDismissed = true;
        }

        // wallet never backed up but backup warning may have been swiped away
        return !warningDismissed && backupTime == 0;
    }

    public Realm getWalletRealm()
    {
        return realmManager.getWalletDataRealmInstance();
    }

    private void storeKeyData(Wallet wallet, Realm r)
    {
        RealmKeyType realmKey = r.where(RealmKeyType.class)
                .equalTo("address", wallet.address, Case.INSENSITIVE)
                .findFirst();

        if (realmKey == null)
        {
            realmKey = r.createObject(RealmKeyType.class, wallet.address);
            if (wallet.walletCreationTime != 0)
            {
                realmKey.setDateAdded(wallet.walletCreationTime);
            }
            else
            {
                realmKey.setDateAdded(System.currentTimeMillis());
            }
        }
        else if (realmKey.getDateAdded() == 0)
        {
            realmKey.setDateAdded(System.currentTimeMillis());
        }

        realmKey.setType(wallet.type);
        realmKey.setLastBackup(wallet.lastBackupTime);
        realmKey.setAuthLevel(wallet.authLevel);
        realmKey.setKeyModulus("");
        r.insertOrUpdate(realmKey);
    }

    private void storeWalletData(Wallet wallet, Realm r)
    {
        RealmWalletData item = r.where(RealmWalletData.class)
                .equalTo("address", wallet.address, Case.INSENSITIVE)
                .findFirst();
        if (item == null) item = r.createObject(RealmWalletData.class, wallet.address);
        item.setName(wallet.name);
        item.setENSName(wallet.ENSname);
        item.setBalance(wallet.balance);
        item.setENSAvatar(wallet.ENSAvatar);
        r.insertOrUpdate(item);
    }

    //Check for lost keystore wallets and recover
    private void recoverLostWallets(Realm realm, Wallet[] keystoreWallets, Map<String, Wallet> walletList, KeyService keyService)
    {
        realm.executeTransaction(r -> {
            for (Wallet w : keystoreWallets)
            {
                //check for orphaned keystore wallets
                Wallet testWallet = walletList.get(w.address.toLowerCase());
                if (testWallet == null)
                {
                    //do we have this in keystore as a valid address?
                    if (keyService.hasKeystore(w.address.toLowerCase()))
                    {
                        //create keystore wallet
                        w.type = WalletType.KEYSTORE;
                        w.authLevel = KeyService.AuthenticationLevel.TEE_AUTHENTICATION;
                        w.lastBackupTime = System.currentTimeMillis();
                        storeKeyData(w, r);
                        walletList.put(w.address.toLowerCase(), w);
                    }
                }
            }

            //try to recover lost HD Wallets
            for (String walletAddr : keyService.detectOrphanedWallets(walletList))
            {
                //create HD Wallet if required
                RealmKeyType realmKey = r.where(RealmKeyType.class)
                        .equalTo("address", walletAddr, Case.INSENSITIVE)
                        .findFirst();

                if (realmKey != null || walletAddr.equals(TokenscriptFunction.ZERO_ADDRESS)) continue;
                Wallet w = new Wallet(walletAddr);
                w.type = WalletType.HDKEY;
                w.authLevel = KeyService.AuthenticationLevel.TEE_AUTHENTICATION;
                w.lastBackupTime = System.currentTimeMillis();
                storeKeyData(w, r);
                walletList.put(w.address.toLowerCase(), w);
            }
        });
    }

    //One-time removal of the WalletTypeRealmInstance usage - this extra database was a
    // workaround for an issue that has since been fixed correctly.
    private void migrateWalletTypeData(Map<String, Wallet> walletList)
    {
        Map<String, Wallet> walletTypeData = new HashMap<>();

        try (Realm realm = realmManager.getWalletTypeRealmInstance())
        {
            //already synced?
            RealmResults<RealmKeyType> rr = realm.where(RealmKeyType.class)
                    .findAll();

            for (RealmKeyType rk : rr)
            {
                Wallet w = composeKeyType(rk);
                if (w != null) walletTypeData.put(w.address.toLowerCase(), w);
            }

            realm.executeTransaction(r -> rr.deleteAllFromRealm()); //erase the database now we have extracted the data
        }

        //Copy results back
        if (walletTypeData.size() > 0)
        {
            try (Realm realm = realmManager.getWalletDataRealmInstance())
            {
                realm.executeTransaction(r -> {
                    //first import data from TypeRealm
                    for (Wallet w : walletList.values())
                    {
                        RealmKeyType data = realm.where(RealmKeyType.class)
                                .equalTo("address", w.address, Case.INSENSITIVE)
                                .findFirst();

                        if (data == null) continue;

                        Wallet walletFromTypeRealm = walletTypeData.get(w.address.toLowerCase());
                        if (walletFromTypeRealm != null)
                        {
                            if (walletFromTypeRealm.walletCreationTime != 0 && walletFromTypeRealm.walletCreationTime < w.walletCreationTime)
                            {
                                data.setDateAdded(walletFromTypeRealm.walletCreationTime);
                            }
                            if (walletFromTypeRealm.lastBackupTime != 0 && w.lastBackupTime == 0)
                            {
                                data.setLastBackup(walletFromTypeRealm.lastBackupTime);
                            }
                            r.insertOrUpdate(data);
                        }
                    }

                    //now copy across any other records
                    for (Wallet w : walletTypeData.values())
                    {
                        if (walletList.get(w.address.toLowerCase()) == null)
                        {
                            //re-introduce this wallet
                            storeKeyData(w, r);
                        }
                    }
                });
            }
        }
    }
}
