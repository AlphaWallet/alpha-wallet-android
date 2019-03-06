package io.stormbird.wallet.repository;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Wallet;
import io.stormbird.wallet.entity.WalletUpdate;
import io.stormbird.wallet.service.AccountKeystoreService;
import io.stormbird.wallet.service.TransactionsNetworkClientType;
import okhttp3.OkHttpClient;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WalletRepository implements WalletRepositoryType
{
	private final PreferenceRepositoryType preferenceRepositoryType;
	private final AccountKeystoreService accountKeystoreService;
	private final EthereumNetworkRepositoryType networkRepository;
	private final TransactionsNetworkClientType blockExplorerClient;
	private final WalletDataRealmSource walletDataRealmSource;
	private final OkHttpClient httpClient;

	public WalletRepository(
			PreferenceRepositoryType preferenceRepositoryType,
			AccountKeystoreService accountKeystoreService,
			EthereumNetworkRepositoryType networkRepository,
			TransactionsNetworkClientType blockExplorerClient,
			WalletDataRealmSource walletDataRealmSource,
			OkHttpClient httpClient) {
		this.preferenceRepositoryType = preferenceRepositoryType;
		this.accountKeystoreService = accountKeystoreService;
		this.networkRepository = networkRepository;
		this.blockExplorerClient = blockExplorerClient;
		this.walletDataRealmSource = walletDataRealmSource;
		this.httpClient = httpClient;
	}

	@Override
	public Single<Wallet[]> fetchWallets()
	{
		return accountKeystoreService.fetchAccounts()
				.flatMap(walletDataRealmSource::populateWalletData);
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
		return Single.fromCallable(() -> {
			//return BigDecimal.valueOf(15.995).movePointRight(18);
			try {
				return new BigDecimal(Web3j
						.build(new HttpService(networkRepository.getDefaultNetwork().rpcServerUrl, httpClient, false))
						.ethGetBalance(wallet.address, DefaultBlockParameterName.PENDING)
						.send()
						.getBalance());
			}
			catch (IOException e)
			{
				return BigDecimal.valueOf(-1);
			}
		}).subscribeOn(Schedulers.io());
	}

	@Override
	public Single<Integer> storeWallets(Wallet[] wallets, boolean isMainNet)
	{
		return walletDataRealmSource.storeWallets(wallets, isMainNet);
	}

	@Override
	public Single<Integer> storeWallet(Wallet wallet)
	{
		return walletDataRealmSource.storeWallet(wallet);
	}

	@Override
	public Single<String> getName(String address) {
		return walletDataRealmSource.getName(address);
	}

	@Override
	public Single<WalletUpdate> scanForNames(Wallet[] wallets, long lastBlockChecked)
	{
		return blockExplorerClient.scanENSTransactionsForWalletNames(wallets, lastBlockChecked);
	}
}