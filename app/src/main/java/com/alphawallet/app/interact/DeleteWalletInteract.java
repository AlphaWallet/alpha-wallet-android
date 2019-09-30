package com.alphawallet.app.interact;

import com.alphawallet.app.repository.WalletRepositoryType;
import com.alphawallet.app.entity.Wallet;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * Delete and fetchTokens wallets
 */
public class DeleteWalletInteract {
	private final WalletRepositoryType walletRepository;

	public DeleteWalletInteract(WalletRepositoryType walletRepository) {
		this.walletRepository = walletRepository;
	}

	public Single<Wallet[]> delete(Wallet wallet)
	{
		return walletRepository.deleteWalletFromRealm(wallet.address)
				.flatMapCompletable(addr -> walletRepository.deleteWallet(addr, ""))
				.andThen(walletRepository.fetchWallets())
				.observeOn(AndroidSchedulers.mainThread());
	}
}
