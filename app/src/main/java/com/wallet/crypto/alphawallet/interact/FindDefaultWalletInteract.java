package com.wallet.crypto.alphawallet.interact;

import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.repository.WalletRepositoryType;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class FindDefaultWalletInteract {

	private final WalletRepositoryType walletRepository;

	public FindDefaultWalletInteract(WalletRepositoryType walletRepository) {
		this.walletRepository = walletRepository;
	}

	public Single<Wallet> find() {
		return walletRepository
				.getDefaultWallet()
				.onErrorResumeNext(walletRepository
						.fetchWallets()
						.to(single -> Flowable.fromArray(single.blockingGet()))
						.firstOrError()
						.flatMapCompletable(walletRepository::setDefaultWallet)
						.andThen(walletRepository.getDefaultWallet()))
				.observeOn(AndroidSchedulers.mainThread());
	}
}
