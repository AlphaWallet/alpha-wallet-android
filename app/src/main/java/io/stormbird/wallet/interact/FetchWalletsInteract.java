package io.stormbird.wallet.interact;

import java.util.Map;

import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.entity.WalletUpdate;
import io.stormbird.wallet.repository.WalletRepositoryType;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class FetchWalletsInteract {

	private final WalletRepositoryType accountRepository;

	public FetchWalletsInteract(WalletRepositoryType accountRepository) {
		this.accountRepository = accountRepository;
	}

	public Single<Wallet[]> fetch(Map<String, Wallet> walletBalances) {
		return accountRepository
				.fetchWallets(walletBalances)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread());
	}

	public Single<Wallet[]> loadWallets()
	{
		return accountRepository.loadWallets();
	}

	public Single<WalletUpdate> scanForNames(Wallet[] wallets, long lastBlockChecked)
	{
		return accountRepository.scanForNames(wallets, lastBlockChecked);
	}

	public Single<Integer> storeWallets(Wallet[] wallets, boolean isMainNet)
	{
		return accountRepository.storeWallets(wallets, isMainNet);
	}

	public Map<String, Wallet> getWalletMap(NetworkInfo network)
	{
		return accountRepository.getWalletMap(network);
	}
}
