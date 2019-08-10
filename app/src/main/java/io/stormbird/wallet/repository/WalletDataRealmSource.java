package io.stormbird.wallet.repository;

import android.util.Log;

import java.util.*;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableCompletableObserver;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.entity.WalletType;
import io.stormbird.wallet.repository.entity.RealmWalletData;
import io.stormbird.wallet.service.KeyService;
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

    public Single<Wallet[]> populateWalletData(Wallet[] wallets, KeyService keyService) {
        return Single.fromCallable(() -> {
            List<Wallet> walletList = new ArrayList<>();
            for (Wallet w : wallets) if (w != null && w.address != null) { w.type = WalletType.KEYSTORE; };

            try (Realm realm = realmManager.getWalletDataRealmInstance())
            {
                for (Wallet hdWallet : keyService.getAllHDWallets())
                {
                    RealmWalletData data = realm.where(RealmWalletData.class)
                            .equalTo("address", hdWallet.address)
                            .findFirst();

                    composeWallet(hdWallet, data);
                    walletList.add(hdWallet);
                }

                for (Wallet keyStoreWallet : wallets)
                {
                    RealmWalletData data = realm.where(RealmWalletData.class)
                            .equalTo("address", keyStoreWallet.address)
                            .findFirst();

                    keyService.checkWalletType(keyStoreWallet);
                    composeWallet(keyStoreWallet, data);
                    walletList.add(keyStoreWallet);
                }

                //finally add watch wallets
                RealmResults<RealmWalletData> realmItems = realm.where(RealmWalletData.class)
                        .sort("lastBackup", Sort.ASCENDING)
                        .equalTo("type", WalletType.WATCH.ordinal())
                        .findAll();

                for (RealmWalletData walletData : realmItems)
                {
                    walletList.add(convertWallet(walletData));
                }
            }

            return walletList.toArray(new Wallet[0]);
        });
    }

    private void composeWallet(Wallet wallet, RealmWalletData d)
    {
        if (d != null)
        {
            wallet.ENSname = d.getENSName();
            wallet.balance = balance(d);
            wallet.name = d.getName();
            wallet.lastBackupTime = d.getLastBackup();
        }
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
        wallet.lastBackupTime = data.getLastBackup();
        wallet.authLevel = data.getAuthLevel();
        wallet.type = data.getType();
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
                        realmWallet.setType(wallet.type);
                        updated++;
                    } else {
                        if (mainNet && (realmWallet.getBalance() == null || !wallet.balance.equals(realmWallet.getENSName())))
                            realmWallet.setBalance(wallet.balance);
                        if (wallet.ENSname != null && (realmWallet.getENSName() == null || !wallet.ENSname.equals(realmWallet.getENSName())))
                            realmWallet.setENSName(wallet.ENSname);
                        realmWallet.setName(wallet.name);
                        realmWallet.setType(wallet.type);
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
                realmWallet.setLastBackup(wallet.lastBackupTime);
                realmWallet.setAuthLevel(wallet.authLevel);

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
                        RealmWalletData realmWallet = realm.where(RealmWalletData.class)
                                .equalTo("address", walletAddr)
                                .findFirst();

                        if (realmWallet != null)
                        {
                            realm.beginTransaction();
                            //Always update warning time but only update backup time if a backup was made
                            if (isBackupTime) realmWallet.setLastBackup(System.currentTimeMillis());
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

    private Single<Long> getWalletTime(String walletAddr, boolean isBackupTime)
    {
        return Single.fromCallable(() -> {
            long backupTime = 0L;
            try (Realm realm = realmManager.getWalletDataRealmInstance()) {
                RealmWalletData realmWallet = realm.where(RealmWalletData.class)
                        .equalTo("address", walletAddr)
                        .findFirst();
                if (realmWallet != null)
                {
                    if (isBackupTime) backupTime = realmWallet.getLastBackup();
                    else backupTime = realmWallet.getLastWarning();
                }
            } catch (Exception e) {
                Log.e(TAG, "getLastBackup: " + e.getMessage(), e);
            }
            return backupTime;
        });
    }

    public Single<Boolean> wasWarningDismissed(String walletAddr)
    {
        return getWalletTime(walletAddr, false)
                .map(time -> (time & 0x1) == 1);
    }

    public Single<String> getWalletRequiresBackup(String wallet)
    {
        return Single.fromCallable(() -> {
            String walletBackup = "";
            try (Realm realm = realmManager.getWalletDataRealmInstance()) {
                RealmWalletData data = realm.where(RealmWalletData.class)
                        .equalTo("address", wallet)
                        .findFirst();

                if (data != null &&
                        !data.getIsDismissedInSettings() &&
                        data.getLastBackup() == 0)
                {
                    walletBackup = wallet;
                }

                // This checks if there's an HD wallet that has never been backed up,
                // and the warning for which hasn't been dismissed within the last dismiss period
//                for (RealmWalletData data : realmItems) {
//                    if (data.getType() == WalletType.HDKEY &&
//                            !data.getIsDismissedInSettings() &&
//                            (data.getLastBackup() == 0 &&
//                                    System.currentTimeMillis() > (data.getLastWarning() + HDKeyService.TIME_BETWEEN_BACKUP_MILLIS)))
//                    {
//                        walletBackup = data.getAddress();
//                        break;
//                    }
//                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return walletBackup;
        });
    }

    public Single<Boolean> getWalletBackupWarning(String walletAddr)
    {
        return Single.fromCallable(() -> {
            long backupTime = 0L;
            long warningTime = 0L;
            try (Realm realm = realmManager.getWalletDataRealmInstance()) {
                RealmWalletData realmWallet = realm.where(RealmWalletData.class)
                        .equalTo("address", walletAddr)
                        .findFirst();
                if (realmWallet != null)
                {
                    backupTime = realmWallet.getLastBackup();
                    warningTime = realmWallet.getLastWarning();
                }
            } catch (Exception e) {
                Log.e(TAG, "getLastBackup: " + e.getMessage(), e);
            }
            return requiresBackup(backupTime, warningTime);
        });
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

    public Single<String> deleteWallet(String walletAddr)
    {
        return Single.fromCallable(() -> {
            try (Realm realm = realmManager.getWalletDataRealmInstance()) {
                RealmWalletData realmWallet = realm.where(RealmWalletData.class)
                        .equalTo("address", walletAddr)
                        .findFirst();

                if (realmWallet != null)
                {
                    realm.beginTransaction();
                    realmWallet.deleteFromRealm();
                    realm.commitTransaction();
                }
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
}
