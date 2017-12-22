package com.wallet.crypto.trustapp.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.service.AccountKeystoreService;

import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.WalletFile;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;

import java.math.BigInteger;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;

import static org.web3j.crypto.Wallet.create;

public class WalletRepository implements WalletRepositoryType {

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
	public Single<Wallet[]> fetchWallets() {
		return accountKeystoreService.fetchAccounts();
	}

	@Override
	public Maybe<Wallet> findWallet(String address) {
		return fetchWallets()
				.flatMapMaybe(accounts -> {
					for (Wallet wallet : accounts) {
						if (wallet.sameAddress(address)) {
							return Maybe.just(wallet);
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
				.flatMapMaybe(this::findWallet)
				.toSingle();
	}

	@Override
	public Maybe<BigInteger> ballanceInWei(Wallet wallet) {
		return Maybe.fromCallable(() -> Web3jFactory
					.build(new HttpService(networkRepository.getDefaultNetwork().infuraUrl))
					.ethGetBalance(wallet.address, DefaultBlockParameterName.LATEST)
					.send()
					.getBalance());
	}
}
