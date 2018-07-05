package io.stormbird.wallet.repository;

import io.reactivex.SingleSource;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.service.AccountKeystoreService;

import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public class WalletRepository implements WalletRepositoryType
{
	private final PreferenceRepositoryType preferenceRepositoryType;
	private final AccountKeystoreService accountKeystoreService;
	private final EthereumNetworkRepositoryType networkRepository;

    public WalletRepository(
			PreferenceRepositoryType preferenceRepositoryType,
			AccountKeystoreService accountKeystoreService,
			EthereumNetworkRepositoryType networkRepository) {
		this.preferenceRepositoryType = preferenceRepositoryType;
		this.accountKeystoreService = accountKeystoreService;
		this.networkRepository = networkRepository;
	}

	@Override
	public Single<Wallet[]> fetchWallets(Map<String, BigDecimal> walletBalances)
	{
		return accountKeystoreService.fetchAccounts()
				.flatMap(wallets -> addBalances(wallets, walletBalances));
	}

	private Single<Wallet[]> addBalances(Wallet[] wallets, Map<String, BigDecimal> walletBalances)
	{
		return Single.fromCallable(() -> {
			if (walletBalances != null)
			{
				for (Wallet w : wallets)
				{
					if (walletBalances.get(w.address) != null) w.setWalletBalance(walletBalances.get(w.address));
				}
			}
			return wallets;
		});
	}

	@Override
	public Single<Wallet> findWallet(String address) {
		return fetchWallets(null)
				.flatMap(accounts -> {
					for (Wallet wallet : accounts) {
						if (wallet.sameAddress(address)) {
							return Single.just(wallet);
						}
					}
					return null;
				});
	}

	@Override
	public Single<Wallet> createWallet(String password) {
		return accountKeystoreService
				.createAccount(password);
	}

	@Override
	public Single<Wallet> importKeystoreToWallet(String store, String password, String newPassword) {
		return accountKeystoreService.importKeystore(store, password, newPassword);
	}

    @Override
    public Single<Wallet> importPrivateKeyToWallet(String privateKey, String newPassword) {
        return accountKeystoreService.importPrivateKey(privateKey, newPassword);
    }

    @Override
	public Single<String> exportWallet(Wallet wallet, String password, String newPassword) {
		return accountKeystoreService.exportAccount(wallet, password, newPassword);
	}

	@Override
	public Completable deleteWallet(String address, String password) {
		return accountKeystoreService.deleteAccount(address, password);
	}

	@Override
	public Completable setDefaultWallet(Wallet wallet) {
		return Completable.fromAction(() -> preferenceRepositoryType.setCurrentWalletAddress(wallet.address));
	}

	@Override
	public Single<Wallet> getDefaultWallet() {
		return Single.fromCallable(preferenceRepositoryType::getCurrentWalletAddress)
				.flatMap(this::findWallet);
	}

	@Override
	public Single<BigDecimal> balanceInWei(Wallet wallet) {
		return Single.fromCallable(() -> {
			//return BigDecimal.valueOf(15.995).movePointRight(18);
			try {
				return new BigDecimal(Web3jFactory
						.build(new org.web3j.protocol.http.HttpService(networkRepository.getDefaultNetwork().rpcServerUrl))
						.ethGetBalance(wallet.address, DefaultBlockParameterName.LATEST)
						.send()
						.getBalance());
			}
			catch (IOException e)
			{
				return BigDecimal.valueOf(-1);
			}
		}).subscribeOn(Schedulers.io());
	}
}