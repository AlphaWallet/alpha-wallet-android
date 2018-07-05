package io.stormbird.wallet.repository;

import io.stormbird.wallet.entity.TradeInstance;
import io.stormbird.wallet.entity.Wallet;

import java.math.BigDecimal;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;

public interface WalletRepositoryType {
	Single<Wallet[]> fetchWallets(Map<String, BigDecimal> walletBalances);
	Single<Wallet> findWallet(String address);

	Single<Wallet> createWallet(String password);
	Single<Wallet> importKeystoreToWallet(String store, String password, String newPassword);
    Single<Wallet> importPrivateKeyToWallet(String privateKey, String newPassword);
	Single<String> exportWallet(Wallet wallet, String password, String newPassword);

	Completable deleteWallet(String address, String password);

	Completable setDefaultWallet(Wallet wallet);
	Single<Wallet> getDefaultWallet();

	Single<BigDecimal> balanceInWei(Wallet wallet);
}
