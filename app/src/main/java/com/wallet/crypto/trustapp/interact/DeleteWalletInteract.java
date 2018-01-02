package com.wallet.crypto.trustapp.interact;

import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.repository.PasswordStore;
import com.wallet.crypto.trustapp.repository.WalletRepositoryType;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * Delete and fetch wallets
 */
public class DeleteWalletInteract {
	private final WalletRepositoryType walletRepository;
	private final PasswordStore passwordStore;

	public DeleteWalletInteract(WalletRepositoryType walletRepository, PasswordStore passwordStore) {
		this.walletRepository = walletRepository;
		this.passwordStore = passwordStore;
	}

	public Single<Wallet[]> delete(Wallet wallet) {
		return passwordStore.getPassword(wallet)
				.flatMapCompletable(password -> walletRepository.deleteWallet(wallet.address, password))
				.andThen(walletRepository.fetchWallets())
				.observeOn(AndroidSchedulers.mainThread());
	}
}
