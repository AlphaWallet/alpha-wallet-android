package io.stormbird.wallet.repository;

import java.math.BigDecimal;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.entity.WalletUpdate;

public interface WalletRepositoryType {
    Single<Wallet[]> fetchWallets();

    Single<Wallet> findWallet(String address);

    Single<Wallet> createWallet(String password);

    Single<Wallet> importKeystoreToWallet(String store, String password, String newPassword);

    Single<Wallet> importPrivateKeyToWallet(String privateKey, String newPassword);

    Single<String> exportWallet(Wallet wallet, String password, String newPassword);

    Completable deleteWallet(String address, String password);

    Completable setDefaultWallet(Wallet wallet);

    Single<Wallet> getDefaultWallet();

    Single<BigDecimal> balanceInWei(Wallet wallet);

    Single<WalletUpdate> scanForNames(Wallet[] wallets, long lastBlockChecked);

    Single<Integer> storeWallets(Wallet[] wallets, boolean isMainNet);

    Single<Integer> storeWallet(Wallet wallet);

    Single<String> getName(String address);
}
