package com.wallet.crypto.trustapp.interact;

import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.repository.WalletRepositoryType;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class FetchWalletsInteract {

	private final WalletRepositoryType accountRepository;

	public FetchWalletsInteract(WalletRepositoryType accountRepository) {
		this.accountRepository = accountRepository;
	}

	public Single<Wallet[]> fetch() {
		return accountRepository
				.fetchWallets()
				.observeOn(AndroidSchedulers.mainThread());
	}
}
