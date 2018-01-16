package com.wallet.crypto.trustapp.repository;

import com.wallet.crypto.trustapp.entity.Wallet;

import java.math.BigInteger;

import io.reactivex.Completable;
import io.reactivex.Single;

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

	Single<BigInteger> balanceInWei(Wallet wallet);
}
