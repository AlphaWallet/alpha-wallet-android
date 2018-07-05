package io.stormbird.wallet.interact;

import java.math.BigDecimal;
import java.util.Map;

import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.repository.WalletRepositoryType;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class FetchWalletsInteract {

	private final WalletRepositoryType accountRepository;

	public FetchWalletsInteract(WalletRepositoryType accountRepository) {
		this.accountRepository = accountRepository;
	}

	public Single<Wallet[]> fetch(Map<String, BigDecimal> walletBalances) {
		return accountRepository
				.fetchWallets(walletBalances)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread());
	}
}
