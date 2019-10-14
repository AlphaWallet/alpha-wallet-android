package com.alphawallet.app.repository;

import java.math.BigDecimal;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;

import com.alphawallet.app.entity.Wallet;

public interface WalletRepositoryType {
    Single<Wallet[]> fetchWallets();

    Single<Wallet> findWallet(String address);

    Single<Wallet> createWallet(String password);

    Single<Wallet> importKeystoreToWallet(String store, String password, String newPassword);

    Single<Wallet> importPrivateKeyToWallet(String privateKey, String newPassword);

    Single<String> exportWallet(Wallet wallet, String password, String newPassword);

    Completable deleteWallet(String address, String password);
    Single<String> deleteWalletFromRealm(String address);

    Completable setDefaultWallet(Wallet wallet);

    Single<Wallet> getDefaultWallet();

    Single<BigDecimal> balanceInWei(Wallet wallet);

    Single<Wallet[]> storeWallets(Wallet[] wallets);

    Single<Wallet> storeWallet(Wallet wallet);
    Single<Wallet> updateWalletData(Wallet wallet);

    Single<String> getName(String address);

    Disposable updateBackupTime(String walletAddr);
    Disposable updateWarningTime(String walletAddr);

    Single<Boolean> getWalletBackupWarning(String walletAddr);

    Single<String> getWalletRequiresBackup(String walletAddr);
    Single<String> setIsDismissed(String walletAddr, boolean isDismissed);

    boolean keystoreExists(String address);
}
