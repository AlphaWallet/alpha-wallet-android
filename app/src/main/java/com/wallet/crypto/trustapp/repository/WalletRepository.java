package com.wallet.crypto.trustapp.repository;

import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.service.AccountKeystoreService;

import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;

import java.math.BigDecimal;
import java.math.BigInteger;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;

public class WalletRepository implements WalletRepositoryType {

	private final PreferenceRepositoryType preferenceRepositoryType;
	private final AccountKeystoreService accountKeystoreService;
	private final EthereumNetworkRepositoryType networkRepository;
    private final OkHttpClient httpClient;

    public WalletRepository(
	        OkHttpClient okHttpClient,
			PreferenceRepositoryType preferenceRepositoryType,
			AccountKeystoreService accountKeystoreService,
			EthereumNetworkRepositoryType networkRepository) {
	    this.httpClient = okHttpClient;
		this.preferenceRepositoryType = preferenceRepositoryType;
		this.accountKeystoreService = accountKeystoreService;
		this.networkRepository = networkRepository;
	}

	@Override
	public Single<Wallet[]> fetchWallets() {
		return accountKeystoreService.fetchAccounts();
	}

	@Override
	public Single<Wallet> findWallet(String address) {
		return fetchWallets()
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
		return Single.fromCallable(() -> new BigDecimal(Web3jFactory
					.build(new HttpService(networkRepository.getDefaultNetwork().rpcServerUrl, httpClient, false))
					.ethGetBalance(wallet.address, DefaultBlockParameterName.LATEST)
					.send()
					.getBalance()))
                .subscribeOn(Schedulers.io());
	}
}