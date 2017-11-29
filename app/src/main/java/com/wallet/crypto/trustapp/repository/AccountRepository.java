package com.wallet.crypto.trustapp.repository;

import com.wallet.crypto.trustapp.entity.Account;
import com.wallet.crypto.trustapp.service.AccountKeystoreService;

import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;

import java.math.BigInteger;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;

public class AccountRepository implements AccountRepositoryType {

	private final PreferenceRepositoryType preferenceRepositoryType;
	private final AccountKeystoreService accountKeystoreService;
	private final EthereumNetworkRepositoryType networkRepository;

	public AccountRepository(
			PreferenceRepositoryType preferenceRepositoryType,
			AccountKeystoreService accountKeystoreService,
			EthereumNetworkRepositoryType networkRepository) {
		this.preferenceRepositoryType = preferenceRepositoryType;
		this.accountKeystoreService = accountKeystoreService;
		this.networkRepository = networkRepository;
	}

	@Override
	public Single<Account[]> fetchAccounts() {
		return accountKeystoreService.fetchAccounts();
	}

	@Override
	public Maybe<Account> findAccount(String address) {
		return fetchAccounts()
				.flatMapMaybe(accounts -> {
					for (Account account : accounts) {
						if (account.sameAddress(address)) {
							return Maybe.just(account);
						}
					}
					return null;
				});
	}

	@Override
	public Single<Account> createAccount(String password) {
		return accountKeystoreService.createAccount(password);
	}

	@Override
	public Single<Account> importAccount(String store, String password) {
		return accountKeystoreService.importStore(store, password);
	}

	@Override
	public Single<String> exportAccount(Account account, String password, String newPassword) {
		return accountKeystoreService.exportAccount(account, password, newPassword);
	}

	@Override
	public Completable deleteAccount(String address, String password) {
		return accountKeystoreService.deleteAccount(address, password);
	}

	@Override
	public Completable setCurrentAccount(Account account) {
		return Completable.fromAction(() -> preferenceRepositoryType.setCurrentAccountAddress(account.address));
	}

	@Override
	public Single<Account> getCurrentAccount() {
		return Single.fromCallable(preferenceRepositoryType::getCurrentAccountAddress)
				.flatMapMaybe(this::findAccount)
				.toSingle();
	}

	@Override
	public Maybe<BigInteger> ballanceInWei(Account account) {
		return Maybe.fromCallable(() -> Web3jFactory
					.build(new HttpService(networkRepository.getDefaultNetwork().infuraUrl))
					.ethGetBalance(account.address, DefaultBlockParameterName.LATEST)
					.send()
					.getBalance());
	}
}
