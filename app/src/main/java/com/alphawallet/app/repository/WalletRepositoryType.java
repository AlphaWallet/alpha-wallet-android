package com.alphawallet.app.repository;

import java.math.BigDecimal;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.realm.Realm;

import com.alphawallet.app.entity.Wallet;

public interface WalletRepositoryType {
    Single<Wallet[]> fetchWallets();

    Single<Wallet> findWallet(String address);

    Single<Wallet> createWallet(String password);

    Single<Wallet> importKeystoreToWallet(String store, String password, String newPassword);

    Single<Wallet> importPrivateKeyToWallet(String privateKey, String newPassword);

    Single<String> exportWallet(Wallet wallet, String password, String newPassword);

    Completable deleteWallet(String address, String password);
    Single<Wallet> deleteWalletFromRealm(Wallet wallet);

    Completable setDefaultWallet(Wallet wallet);

    Single<Wallet> getDefaultWallet();

    Single<Wallet[]> storeWallets(Wallet[] wallets);

    Single<Wallet> storeWallet(Wallet wallet);
    void updateWalletData(Wallet wallet, Realm.Transaction.OnSuccess onSuccess);

    Single<String> getName(String address);

    void updateBackupTime(String walletAddr);
    void updateWarningTime(String walletAddr);

    Single<Boolean> getWalletBackupWarning(String walletAddr);

    Single<String> getWalletRequiresBackup(String walletAddr);
    void setIsDismissed(String walletAddr, boolean isDismissed);

    boolean keystoreExists(String address);

    Realm getWalletRealm();
}
