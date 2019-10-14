package com.alphawallet.app.repository;

import android.util.Log;

import com.alphawallet.app.repository.entity.RealmKeyType;
import com.alphawallet.app.repository.entity.RealmWalletData;

import java.util.*;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableCompletableObserver;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.WalletType;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.RealmManager;

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

    public Single<Wallet[]> populateWalletData(Wallet[] wallets, KeyService keyService) {
        return Single.fromCallable(() -> {
            List<Wallet> walletList = loadOrCreateKeyRealmDB(wallets, keyService); //call has action on upgrade to new UX

            try (Realm realm = realmManager.getWalletDataRealmInstance())
            {
                //Add additional - non critical wallet data. This database can be voided for upgrade if required
                for (Wallet wallet : walletList)
                {
                    RealmWalletData data = realm.where(RealmWalletData.class)
                            .equalTo("address", wallet.address)
                            .findFirst();
                    composeWallet(wallet, data);
                }
            }

            return walletList.toArray(new Wallet[0]);
        });
    }

    private List<Wallet> loadOrCreateKeyRealmDB(Wallet[] wallets,KeyService keyService)
    {
        List<Wallet> walletList = new ArrayList<>();
        try (Realm realm = realmManager.getWalletTypeRealmInstance())
        {
            RealmResults<RealmKeyType> realmKeyTypes = realm.where(RealmKeyType.class)
                    .sort("dateAdded", Sort.ASCENDING)
                    .findAll();

            if (realmKeyTypes.size() > 0)
            {
                //Load fixed wallet data: wallet type, creation and backup times
                for (RealmKeyType walletTypeData : realmKeyTypes)
                {
                    Wallet w = composeKeyType(walletTypeData);
                    if (w != null) walletList.add(w);
                }
            }
            else //only zero on upgrade from v2.01.3 and lower (pre-HD key)
            {
                realm.beginTransaction();
                for (Wallet wallet : wallets)
                {
                    RealmKeyType realmKey = realm.createObject(RealmKeyType.class, wallet.address);
                    wallet.authLevel = KeyService.AuthenticationLevel.TEE_NO_AUTHENTICATION;
                    wallet.type = WalletType.KEYSTORE_LEGACY;
                    realmKey.setType(wallet.type); //all keys are legacy
                    realmKey.setLastBackup(System.currentTimeMillis());
                    realmKey.setDateAdded(wallet.walletCreationTime);
                    realmKey.setAuthLevel(wallet.authLevel);
                    walletList.add(wallet);
                }
                realm.commitTransaction();
            }
        }
        return walletList;
    }

    private void composeWallet(Wallet wallet, RealmWalletData d)
    {
        if (d != null)
        {
            wallet.ENSname = d.getENSName();
            wallet.balance = balance(d);
            wallet.name = d.getName();
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

    private Wallet convertWallet(RealmWalletData data) {
        Wallet wallet = new Wallet(data.getAddress());
        wallet.ENSname = data.getENSName();
        wallet.balance = data.getBalance();
        wallet.name = data.getName();
        return wallet;
    }

    public Single<Wallet[]> storeWallets(Wallet[] wallets) {
        return Single.fromCallable(() -> {
            try (Realm realm = realmManager.getWalletDataRealmInstance()) {
                realm.beginTransaction();

                for (Wallet wallet : wallets) {
                    RealmWalletData realmWallet = realm.where(RealmWalletData.class)
                            .equalTo("address", wallet.address)
                            .findFirst();

                    if (realmWallet == null) {
                        realmWallet = realm.createObject(RealmWalletData.class, wallet.address);
                        realmWallet.setENSName(wallet.ENSname);
                        realmWallet.setBalance(wallet.balance);
                        realmWallet.setName(wallet.name);
                    } else {
                        if (realmWallet.getBalance() == null || !wallet.balance.equals(realmWallet.getENSName()))
                            realmWallet.setBalance(wallet.balance);
                        if (wallet.ENSname != null && (realmWallet.getENSName() == null || !wallet.ENSname.equals(realmWallet.getENSName())))
                            realmWallet.setENSName(wallet.ENSname);
                        realmWallet.setName(wallet.name);
                    }
                }
                realm.commitTransaction();
            } catch (Exception e) {
                Log.e(TAG, "storeWallets: " + e.getMessage(), e);
            }
            return wallets;
        });
    }

    public Single<Wallet> storeWallet(Wallet wallet) {
        return Single.fromCallable(() -> {
            storeKeyData(wallet);
            storeWalletData(wallet);
            return wallet;
        });
    }

    public Single<Wallet> updateWalletData(Wallet wallet) {
        return Single.fromCallable(() -> {
            storeWalletData(wallet);
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

    private Disposable updateTimeInternal(String walletAddr, boolean isBackupTime)
    {
        return Completable.complete()
                .subscribeWith(new DisposableCompletableObserver()
                {
                    Realm realm;
                    @Override
                    public void onStart()
                    {
                        realm = realmManager.getWalletDataRealmInstance();
                        realm.beginTransaction();
                        if (isBackupTime)
                        {
                            setKeyBackupTime(walletAddr);
                        }

                        RealmWalletData realmWallet = realm.where(RealmWalletData.class)
                                .equalTo("address", walletAddr)
                                .findFirst();

                        if (realmWallet != null)
                        {
                            //Always update warning time but only update backup time if a backup was made
                            realmWallet.setLastWarning(System.currentTimeMillis());
                        }
                    }

                    @Override
                    public void onComplete()
                    {
                        if (realm.isInTransaction()) realm.commitTransaction();
                        realm.close();
                    }

                    @Override
                    public void onError(Throwable e)
                    {
                        if (realm != null && !realm.isClosed())
                        {
                            realm.close();
                        }
                    }
                });
    }

    public Disposable updateBackupTime(String walletAddr)
    {
        return updateTimeInternal(walletAddr, true);
    }

    public Disposable updateWarningTime(String walletAddr)
    {
        return updateTimeInternal(walletAddr, false);
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

    public Single<String> deleteWallet(String walletAddr)
    {
        return Single.fromCallable(() -> {
            try (Realm realm = realmManager.getWalletDataRealmInstance())
            {
                RealmWalletData realmWallet = realm.where(RealmWalletData.class).equalTo("address", walletAddr).findFirst();
                realm.beginTransaction();
                if (realmWallet != null) realmWallet.deleteFromRealm();
                realm.commitTransaction();
            }
            try (Realm realm = realmManager.getWalletTypeRealmInstance())
            {
                RealmKeyType realmKey = realm.where(RealmKeyType.class).equalTo("address", walletAddr).findFirst();
                realm.beginTransaction();
                if (realmKey != null) realmKey.deleteFromRealm();
                realm.commitTransaction();
            }
            return walletAddr;
        });
    }

    public Single<String> setIsDismissed(String walletAddr, boolean isDismissed)
    {
        return Single.fromCallable(() -> {
            try (Realm realm = realmManager.getWalletDataRealmInstance()) {
                RealmWalletData realmWallet = realm.where(RealmWalletData.class)
                        .equalTo("address", walletAddr)
                        .findFirst();

                if (realmWallet != null)
                {
                    realm.beginTransaction();
                    realmWallet.setIsDismissedInSettings(isDismissed);
                    realm.commitTransaction();
                }
            }
            return walletAddr;
        });
    }


    private void storeKeyData(Wallet wallet)
    {
        try (Realm realm = realmManager.getWalletTypeRealmInstance())
        {
            RealmKeyType realmKey = realm.where(RealmKeyType.class)
                    .equalTo("address", wallet.address)
                    .findFirst();

            realm.beginTransaction();
            if (realmKey == null) {
                realmKey = realm.createObject(RealmKeyType.class, wallet.address);
                realmKey.setDateAdded(System.currentTimeMillis());
            }
            else if (realmKey.getDateAdded() == 0) realmKey.setDateAdded(System.currentTimeMillis());

            realmKey.setType(wallet.type);
            realmKey.setLastBackup(wallet.lastBackupTime);
            realmKey.setAuthLevel(wallet.authLevel);
            realmKey.setKeyModulus("");
            realm.commitTransaction();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void storeWalletData(Wallet wallet)
    {
        try (Realm realm = realmManager.getWalletDataRealmInstance())
        {
            RealmWalletData realmWallet = realm.where(RealmWalletData.class)
                    .equalTo("address", wallet.address)
                    .findFirst();

            realm.beginTransaction();
            if (realmWallet == null) {
                realmWallet = realm.createObject(RealmWalletData.class, wallet.address);
            }

            realmWallet.setName(wallet.name);
            realmWallet.setENSName(wallet.ENSname);
            realmWallet.setBalance(wallet.balance);
            realm.commitTransaction();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void setKeyBackupTime(String walletAddr)
    {
        try (Realm realm = realmManager.getWalletTypeRealmInstance()) {
            RealmKeyType realmKey = realm.where(RealmKeyType.class)
                    .equalTo("address", walletAddr)
                    .findFirst();

            if (realmKey != null)
            {
                realm.beginTransaction();
                realmKey.setLastBackup(System.currentTimeMillis());
                realm.commitTransaction();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isDismissedInSettings(String wallet)
    {
        try (Realm realm = realmManager.getWalletDataRealmInstance()) {
            RealmWalletData data = realm.where(RealmWalletData.class)
                    .equalTo("address", wallet)
                    .findFirst();
            return data != null && data.getIsDismissedInSettings();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private long getWalletWarningTime(String walletAddr)
    {
        try (Realm realm = realmManager.getWalletDataRealmInstance()) {
            RealmWalletData data = realm.where(RealmWalletData.class)
                    .equalTo("address", walletAddr)
                    .findFirst();

            if (data != null)
            {
                return data.getLastWarning();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    private long getKeyBackupTime(String walletAddr)
    {
        try (Realm realm = realmManager.getWalletTypeRealmInstance()) {
            RealmKeyType realmKey = realm.where(RealmKeyType.class)
                    .equalTo("address", walletAddr)
                    .findFirst();

            if (realmKey != null)
            {
                return realmKey.getLastBackup();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
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
}
