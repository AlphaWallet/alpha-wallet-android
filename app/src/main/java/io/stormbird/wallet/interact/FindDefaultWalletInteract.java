package io.stormbird.wallet.interact;

import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.repository.WalletRepositoryType;

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
						.fetchWallets(null)
						.to(single -> Flowable.fromArray(single.blockingGet()))
						.firstOrError()
						.flatMapCompletable(walletRepository::setDefaultWallet)
						.andThen(walletRepository.getDefaultWallet()))
				.observeOn(AndroidSchedulers.mainThread());
	}
}
