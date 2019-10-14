package com.alphawallet.app.repository;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigDecimal;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.service.AccountKeystoreService;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.TransactionsNetworkClientType;
import okhttp3.OkHttpClient;

public class WalletRepository implements WalletRepositoryType
{
	private final PreferenceRepositoryType preferenceRepositoryType;
	private final AccountKeystoreService accountKeystoreService;
	private final EthereumNetworkRepositoryType networkRepository;
	private final TransactionsNetworkClientType blockExplorerClient;
	private final WalletDataRealmSource walletDataRealmSource;
	private final OkHttpClient httpClient;
	private final KeyService keyService;

	public WalletRepository(PreferenceRepositoryType preferenceRepositoryType, AccountKeystoreService accountKeystoreService, EthereumNetworkRepositoryType networkRepository, TransactionsNetworkClientType blockExplorerClient, WalletDataRealmSource walletDataRealmSource, OkHttpClient httpClient, KeyService keyService)
	{
		this.preferenceRepositoryType = preferenceRepositoryType;
		this.accountKeystoreService = accountKeystoreService;
		this.networkRepository = networkRepository;
		this.blockExplorerClient = blockExplorerClient;
		this.walletDataRealmSource = walletDataRealmSource;
		this.httpClient = httpClient;
		this.keyService = keyService;
	}

	@Override
	public Single<Wallet[]> fetchWallets()
	{
		return accountKeystoreService.fetchAccounts().flatMap(wallets -> walletDataRealmSource.populateWalletData(wallets, keyService));
	}

	@Override
	public Single<Wallet> findWallet(String address)
	{
		return fetchWallets().flatMap(accounts -> {
			for (Wallet wallet : accounts)
			{
				if (wallet.sameAddress(address))
				{
					return Single.just(wallet);
				}
			}
			return null;
		});
	}

	@Override
	public Single<Wallet> createWallet(String password)
	{
		return accountKeystoreService.createAccount(password);
	}

	@Override
	public Single<Wallet> importKeystoreToWallet(String store, String password, String newPassword)
	{
		return accountKeystoreService.importKeystore(store, password, newPassword);
	}

	@Override
	public Single<Wallet> importPrivateKeyToWallet(String privateKey, String newPassword)
	{
		return accountKeystoreService.importPrivateKey(privateKey, newPassword);
	}

	@Override
	public Single<String> exportWallet(Wallet wallet, String password, String newPassword)
	{
		return accountKeystoreService.exportAccount(wallet, password, newPassword);
	}

	@Override
	public Completable deleteWallet(String address, String password)
	{
		return accountKeystoreService.deleteAccount(address, password);
	}

	@Override
	public Single<String> deleteWalletFromRealm(String address)
	{
		return walletDataRealmSource.deleteWallet(address);
	}

	@Override
	public Completable setDefaultWallet(Wallet wallet)
	{
		return Completable.fromAction(() -> preferenceRepositoryType.setCurrentWalletAddress(wallet.address));
	}

	@Override
	public Disposable updateBackupTime(String walletAddr)
	{
		return walletDataRealmSource.updateBackupTime(walletAddr);
	}

	@Override
	public Disposable updateWarningTime(String walletAddr)
	{
		return walletDataRealmSource.updateWarningTime(walletAddr);
	}

	@Override
	public Single<Boolean> getWalletBackupWarning(String walletAddr)
	{
		return walletDataRealmSource.getWalletBackupWarning(walletAddr);
	}

	@Override
	public Single<String> getWalletRequiresBackup(String walletAddr)
	{
		return walletDataRealmSource.getWalletRequiresBackup(walletAddr);
	}

	@Override
	public Single<String> setIsDismissed(String walletAddr, boolean isDismissed)
	{
		return walletDataRealmSource.setIsDismissed(walletAddr, isDismissed);
	}

	@Override
	public Single<Wallet> getDefaultWallet()
	{
		return Single.fromCallable(preferenceRepositoryType::getCurrentWalletAddress).flatMap(this::findWallet);
	}

	@Override
	public Single<BigDecimal> balanceInWei(Wallet wallet)
	{
		return Single.fromCallable(() -> {
			//return BigDecimal.valueOf(15.995).movePointRight(18);
			try
			{
				return new BigDecimal(Web3j.build(new HttpService(networkRepository.getDefaultNetwork().rpcServerUrl, httpClient, false)).ethGetBalance(wallet.address, DefaultBlockParameterName.PENDING).send().getBalance());
			}
			catch (IOException e)
			{
				return BigDecimal.valueOf(-1);
			}
		}).subscribeOn(Schedulers.io());
	}

	@Override
	public Single<Wallet[]> storeWallets(Wallet[] wallets)
	{
		return walletDataRealmSource.storeWallets(wallets);
	}

	@Override
	public Single<Wallet> storeWallet(Wallet wallet)
	{
		return walletDataRealmSource.storeWallet(wallet);
	}

	@Override
	public Single<Wallet> updateWalletData(Wallet wallet)
	{
		return walletDataRealmSource.updateWalletData(wallet);
	}

	@Override
	public Single<String> getName(String address)
	{
		return walletDataRealmSource.getName(address);
	}

	@Override
    public boolean keystoreExists(String address)
    {
        return accountKeystoreService.hasAccount(address);
    }
}