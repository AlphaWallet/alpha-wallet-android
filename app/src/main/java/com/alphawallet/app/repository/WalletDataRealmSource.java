package com.alphawallet.app.repository;

import android.text.TextUtils;
import android.util.Pair;

import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.repository.entity.RealmKeyType;
import com.alphawallet.app.repository.entity.RealmWalletData;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.RealmManager;

import org.web3j.crypto.WalletUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.realm.kotlin.Realm;
import io.realm.kotlin.TypedRealm;
import com.alphawallet.app.realm.RealmCompat;
import com.alphawallet.app.realm.AutoCloseableRealm;
import com.alphawallet.app.realm.RealmCompat.Results;
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
            try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getWalletDataRealmInstance()))
            {
            Realm realm = realmCloser.getRealm();
                walletList = loadOrCreateKeyRealmDB(realm, keystoreWallets, keyService); //call has action on upgrade to new UX
                //Add additional - non critical wallet data. This database can be voided for upgrade if required
                for (Wallet wallet : walletList.values())
                {
                    RealmWalletData data = RealmCompat.where(realm, RealmWalletData.class)
                            .equalTo("address", wallet.address, RealmCompat.Case.INSENSITIVE)
                            .findFirst();

                    composeWallet(wallet, data);
                }
            }

            migrateWalletTypeData(walletList, keyService);

            Timber.tag("RealmDebug").d("populate %s", walletList.size());
            return walletList.values().toArray(new Wallet[0]);
        });
    }

    private Map<String, Wallet> loadOrCreateKeyRealmDB(TypedRealm realm, Wallet[] wallets, KeyService keyService)
    {
        Map<String, Wallet> walletList = new HashMap<>();
        List<String> keyStoreList = walletArrayToAddressList(wallets);

        RealmCompat.executeTransaction(realm, r -> {
            RealmCompat.Results<RealmKeyType> orphanKeys = RealmCompat.where(r, RealmKeyType.class)
                    .equalTo("address", "")
                    .findAll();
            if (!orphanKeys.isEmpty()) {
                orphanKeys.deleteAllFromRealm(r);
            }
        });

        RealmCompat.refresh(realm); //ensure we're fully up to date before query
        RealmCompat.Results<RealmKeyType> realmKeyTypes = RealmCompat.where(realm, RealmKeyType.class)
                .sort("dateAdded", RealmCompat.Sort.ASCENDING)
                .findAll();

        List<Wallet> walletUpdates = new ArrayList<>();

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

                if (w.type == WalletType.KEYSTORE_LEGACY && !testLegacyCipher(w, keyService))
                {
                    w.type = WalletType.KEYSTORE;
                    walletUpdates.add(w);
                }

                walletList.put(w.address.toLowerCase(), w);
            }
        }
        else //only zero on upgrade from v2.01.3 and lower (pre-HD key)
        {
            for (Wallet wallet : wallets)
            {
                wallet.authLevel = KeyService.AuthenticationLevel.TEE_NO_AUTHENTICATION;
                if (testLegacyCipher(wallet, keyService))
                {
                    wallet.type = WalletType.KEYSTORE_LEGACY;
                }
                else
                {
                    wallet.type = WalletType.KEYSTORE;
                }
                walletList.put(wallet.address.toLowerCase(), wallet);
                walletUpdates.add(wallet);
            }
        }

        recoverOrphanedKeys(walletList, walletUpdates, keyService);

        if (walletUpdates.size() > 0)
        {
            storeWallets(realm, walletUpdates.toArray(new Wallet[0]));
        }

        Timber.tag("RealmDebug").d("loadorcreate %s", walletList.size());
        return walletList;
    }

    /**
     * Re-registers wallets whose RealmKeyType record was lost (eg through an inconsistent
     * OEM backup/restore, or a failed database migration) but whose encrypted key file, iv file
     * and Android Keystore alias are still intact on disk. See
     * {@link KeyService#findRecoverableOrphanedKeys} for the safety checks performed.
     */
    private void recoverOrphanedKeys(Map<String, Wallet> walletList, List<Wallet> walletUpdates, KeyService keyService)
    {
        List<String> orphanedAddresses = keyService.findRecoverableOrphanedKeys(walletList.keySet());
        for (String address : orphanedAddresses)
        {
            Wallet w = new Wallet(address);
            w.type = WalletType.HDKEY;
            w.walletCreationTime = System.currentTimeMillis();
            w.authLevel = KeyService.AuthenticationLevel.NOT_SET; //original auth level was lost along with the record

            walletList.put(w.address.toLowerCase(), w);
            walletUpdates.add(w);
            Timber.tag("RealmDebug").w("Recovered orphaned wallet key with intact key material: %s", address);
        }
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
        if (keyType != null && !TextUtils.isEmpty(keyType.getAddress()) && WalletUtils.isValidAddress(keyType.getAddress()))
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

    public Single<Wallet[]> storeWallets(Wallet[] wallets)
    {
        return Single.fromCallable(() -> {
            try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getWalletDataRealmInstance()))
            {
            Realm realm = realmCloser.getRealm();
                storeWallets(realm, wallets);
            }
            catch (Exception e)
            {
                Timber.e(e, "storeWallets: %s", e.getMessage());
            }
            return wallets;
        });
    }

    private void storeWallets(TypedRealm realm, Wallet[] wallets)
    {
        RealmCompat.executeTransaction(realm, r -> {
            for (Wallet wallet : wallets)
            {
                storeKeyData(wallet, r);
                storeWalletData(wallet, r);
            }
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

    public void updateWalletData(Wallet wallet, RealmCompat.OnSuccess onSuccess)
    {
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getWalletDataRealmInstance()))
        {
            Realm realm = realmCloser.getRealm();
            RealmCompat.executeTransactionAsync(realm, r -> {
                storeKeyData(wallet, r);
                storeWalletData(wallet, r);
                Timber.tag("RealmDebug").d("storedKeydata %s", wallet.address);
            }, onSuccess);
        }
        catch (Exception e)
        {
            Timber.e(e);
            onSuccess.onSuccess();
        }
    }

    public void updateWalletItem(Wallet wallet, WalletItem item, RealmCompat.OnSuccess onSuccess)
    {
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getWalletDataRealmInstance()))
        {
            Realm realm = realmCloser.getRealm();
            RealmCompat.executeTransactionAsync(realm, r -> {
                RealmWalletData walletData = RealmCompat.where(r, RealmWalletData.class)
                        .equalTo("address", wallet.address, RealmCompat.Case.INSENSITIVE)
                        .findFirst();

                if (walletData != null)
                {
                    switch (item)
                    {
                        case NAME:
                            walletData.setName(wallet.name);
                            break;
                        case ENS_NAME:
                            walletData.setENSName(wallet.ENSname);
                            break;
                        case BALANCE:
                            walletData.setBalance(wallet.balance);
                            break;
                        case ENS_AVATAR:
                            walletData.setENSAvatar(wallet.ENSAvatar);
                            break;
                    }

                    RealmCompat.insertOrUpdate(r, walletData);
                }
                Timber.tag("RealmDebug").d("storedKeydata %s", wallet.address);
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
            try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getWalletDataRealmInstance())) {
            Realm realm = realmCloser.getRealm();
                RealmWalletData realmWallet = RealmCompat.where(realm, RealmWalletData.class)
                        .equalTo("address", address, RealmCompat.Case.INSENSITIVE)
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

        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getWalletDataRealmInstance()))
        {
            Realm realm = realmCloser.getRealm();
            RealmCompat.executeTransactionAsync(realm, r -> {
                RealmKeyType realmKey = RealmCompat.where(r, RealmKeyType.class)
                        .equalTo("address", walletAddr, RealmCompat.Case.INSENSITIVE)
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
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getWalletDataRealmInstance()))
        {
            Realm realm = realmCloser.getRealm();
            RealmCompat.executeTransactionAsync(realm, r -> {
                RealmWalletData realmWallet = RealmCompat.where(r, RealmWalletData.class)
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
            try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getWalletDataRealmInstance()))
            {
            Realm realm = realmCloser.getRealm();
                RealmCompat.executeTransaction(realm, r -> {
                    RealmCompat.Results<RealmWalletData> realmWallet = RealmCompat.where(r, RealmWalletData.class).equalTo("address", wallet.address, RealmCompat.Case.INSENSITIVE).findAll();
                    if (realmWallet != null) realmWallet.deleteAllFromRealm(r);
                    RealmCompat.Results<RealmKeyType> realmKey = RealmCompat.where(r, RealmKeyType.class).equalTo("address", wallet.address, RealmCompat.Case.INSENSITIVE).findAll();
                    if (realmKey != null) realmKey.deleteAllFromRealm(r);
                });
            }
            catch (Exception e)
            {
                Timber.e(e);
            }

            try (AutoCloseableRealm instanceCloser = AutoCloseableRealm.wrap(realmManager.getRealmInstance(wallet)))
            {
            Realm instance = instanceCloser.getRealm();
                RealmCompat.executeTransaction(instance, r -> RealmCompat.deleteAll(r));
                RealmCompat.refresh(instance);
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
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getWalletDataRealmInstance()))
        {
            Realm realm = realmCloser.getRealm();
            RealmCompat.executeTransactionAsync(realm, r -> {
                RealmWalletData realmWallet = RealmCompat.where(r, RealmWalletData.class)
                        .equalTo("address", walletAddr, RealmCompat.Case.INSENSITIVE)
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
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getWalletDataRealmInstance()))
        {
            Realm realm = realmCloser.getRealm();
            RealmCompat.executeTransaction(realm, r -> {
                storeKeyData(wallet, r);
                Timber.tag("RealmDebug").d("storedKeyData %s", wallet.address);
            });
        }
    }

    private void storeWalletData(Wallet wallet)
    {
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getWalletDataRealmInstance()))
        {
            Realm realm = realmCloser.getRealm();
            RealmCompat.executeTransaction(realm, r -> {
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
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getWalletDataRealmInstance()))
        {
            Realm realm = realmCloser.getRealm();
            RealmWalletData data = RealmCompat.where(realm, RealmWalletData.class)
                    .equalTo("address", wallet, RealmCompat.Case.INSENSITIVE)
                    .findFirst();
            return data != null && data.getIsDismissedInSettings();
        }
    }

    private long getWalletWarningTime(String walletAddr)
    {
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getWalletDataRealmInstance()))
        {
            Realm realm = realmCloser.getRealm();
            RealmWalletData data = RealmCompat.where(realm, RealmWalletData.class)
                    .equalTo("address", walletAddr, RealmCompat.Case.INSENSITIVE)
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
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getWalletDataRealmInstance()))
        {
            Realm realm = realmCloser.getRealm();
            RealmKeyType realmKey = RealmCompat.where(realm, RealmKeyType.class)
                    .equalTo("address", walletAddr, RealmCompat.Case.INSENSITIVE)
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

    private void storeKeyData(Wallet wallet, TypedRealm r)
    {
        if (android.text.TextUtils.isEmpty(wallet.address))
        {
            return;
        }

        RealmKeyType realmKey = RealmCompat.where(r, RealmKeyType.class)
                .equalTo("address", wallet.address, RealmCompat.Case.INSENSITIVE)
                .findFirst();

        if (realmKey == null)
        {
            realmKey = RealmCompat.createObject(r, RealmKeyType.class, wallet.address);
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
        RealmCompat.insertOrUpdate(r, realmKey);
    }

    private void storeWalletData(Wallet wallet, TypedRealm r)
    {
        if (android.text.TextUtils.isEmpty(wallet.address))
        {
            return;
        }

        RealmWalletData item = RealmCompat.where(r, RealmWalletData.class)
                .equalTo("address", wallet.address, RealmCompat.Case.INSENSITIVE)
                .findFirst();
        if (item == null) item = RealmCompat.createObject(r, RealmWalletData.class, wallet.address);
        item.setName(wallet.name);
        item.setENSName(wallet.ENSname);
        item.setBalance(wallet.balance);
        item.setENSAvatar(wallet.ENSAvatar);
        RealmCompat.insertOrUpdate(r, item);
    }

    private boolean testLegacyCipher(Wallet w, KeyService service)
    {
        //test for legacy cipher, any failure we know it's a KEYSTORE
        Pair<KeyService.KeyExceptionType, String> cipherTest = service.testCipher(w.address, KeyService.LEGACY_CIPHER_ALGORITHM);
        return cipherTest.first == KeyService.KeyExceptionType.SUCCESSFUL_DECODE;
    }

    //One-time removal of the WalletTypeRealmInstance usage - this extra database was a
    // workaround for an issue that has since been fixed correctly.
    private void migrateWalletTypeData(Map<String, Wallet> walletList, KeyService service)
    {
        Map<String, Wallet> walletTypeData = new HashMap<>();

        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getWalletTypeRealmInstance()))
        {
            Realm realm = realmCloser.getRealm();
            //already synced?
            RealmCompat.Results<RealmKeyType> rr = RealmCompat.where(realm, RealmKeyType.class)
                    .findAll();

            for (RealmKeyType rk : rr)
            {
                Wallet w = composeKeyType(rk);
                if (w != null)
                {
                    walletTypeData.put(w.address.toLowerCase(), w);
                    if (w.type == WalletType.KEYSTORE_LEGACY && !testLegacyCipher(w, service))
                    {
                        w.type = WalletType.KEYSTORE;
                    }
                }
            }
        }

        //Copy results back
        if (walletTypeData.size() > 0)
        {
            try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getWalletDataRealmInstance()))
            {
            Realm realm = realmCloser.getRealm();
                RealmCompat.executeTransaction(realm, r -> {
                    //first import data from TypeRealm
                    for (Wallet w : walletList.values())
                    {
                        RealmKeyType data = RealmCompat.where(realm, RealmKeyType.class)
                                .equalTo("address", w.address, RealmCompat.Case.INSENSITIVE)
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
                            RealmCompat.insertOrUpdate(r, data);
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

            deleteWalletTypeData();
        }
    }

    private void deleteWalletTypeData()
    {
        //now that process has completed, delete the records
        try (AutoCloseableRealm realmCloser = AutoCloseableRealm.wrap(realmManager.getWalletTypeRealmInstance()))
        {
            Realm realm = realmCloser.getRealm();
            //already synced?
            RealmCompat.Results<RealmKeyType> rr = RealmCompat.where(realm, RealmKeyType.class)
                    .findAll();
            RealmCompat.executeTransaction(realm, r -> rr.deleteAllFromRealm(r)); //erase the database now we have extracted the data
        }
    }
}
