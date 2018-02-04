package com.wallet.crypto.alphawallet.di;

import android.content.Context;

import com.google.gson.Gson;
import com.wallet.crypto.alphawallet.repository.EthereumNetworkRepository;
import com.wallet.crypto.alphawallet.repository.EthereumNetworkRepositoryType;
import com.wallet.crypto.alphawallet.repository.GasSettingsRepository;
import com.wallet.crypto.alphawallet.repository.GasSettingsRepositoryType;
import com.wallet.crypto.alphawallet.repository.PreferenceRepositoryType;
import com.wallet.crypto.alphawallet.repository.SharedPreferenceRepository;
import com.wallet.crypto.alphawallet.repository.TokenLocalSource;
import com.wallet.crypto.alphawallet.repository.TokenRepository;
import com.wallet.crypto.alphawallet.repository.TokenRepositoryType;
import com.wallet.crypto.alphawallet.repository.TokensRealmSource;
import com.wallet.crypto.alphawallet.repository.TransactionLocalSource;
import com.wallet.crypto.alphawallet.repository.TransactionRepository;
import com.wallet.crypto.alphawallet.repository.TransactionRepositoryType;
import com.wallet.crypto.alphawallet.repository.TransactionsRealmCache;
import com.wallet.crypto.alphawallet.repository.WalletRepository;
import com.wallet.crypto.alphawallet.repository.WalletRepositoryType;
import com.wallet.crypto.alphawallet.service.AccountKeystoreService;
import com.wallet.crypto.alphawallet.service.CoinmarketcapTickerService;
import com.wallet.crypto.alphawallet.service.EthplorerTokenService;
import com.wallet.crypto.alphawallet.service.GethKeystoreAccountService;
import com.wallet.crypto.alphawallet.service.RealmManager;
import com.wallet.crypto.alphawallet.service.TickerService;
import com.wallet.crypto.alphawallet.service.TokenExplorerClientType;
import com.wallet.crypto.alphawallet.service.TransactionsNetworkClient;
import com.wallet.crypto.alphawallet.service.TransactionsNetworkClientType;
import com.wallet.crypto.alphawallet.service.TrustWalletTickerService;

import java.io.File;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;

@Module
public class RepositoriesModule {
	@Singleton
	@Provides
	PreferenceRepositoryType providePreferenceRepository(Context context) {
		return new SharedPreferenceRepository(context);
	}

	@Singleton
	@Provides
	AccountKeystoreService provideAccountKeyStoreService(Context context) {
        File file = new File(context.getFilesDir(), "keystore/keystore");
		return new GethKeystoreAccountService(file);
	}

	@Singleton
    @Provides
    TickerService provideTickerService(OkHttpClient httpClient, Gson gson) {
	    //return new TrustWalletTickerService(httpClient, gson);
		return new CoinmarketcapTickerService(httpClient, gson);
    }

	@Singleton
	@Provides
	EthereumNetworkRepositoryType provideEthereumNetworkRepository(
            PreferenceRepositoryType preferenceRepository,
            TickerService tickerService) {
		return new EthereumNetworkRepository(preferenceRepository, tickerService);
	}

	@Singleton
	@Provides
    WalletRepositoryType provideWalletRepository(
            OkHttpClient okHttpClient,
			PreferenceRepositoryType preferenceRepositoryType,
			AccountKeystoreService accountKeystoreService,
			EthereumNetworkRepositoryType networkRepository) {
		return new WalletRepository(
		        okHttpClient, preferenceRepositoryType, accountKeystoreService, networkRepository);
	}

	@Singleton
	@Provides
	TransactionRepositoryType provideTransactionRepository(
			EthereumNetworkRepositoryType networkRepository,
			AccountKeystoreService accountKeystoreService,
			TransactionsNetworkClientType blockExplorerClient,
            TransactionLocalSource inDiskCache) {
		return new TransactionRepository(
				networkRepository,
				accountKeystoreService,
				inDiskCache,
				blockExplorerClient);
	}

	@Singleton
    @Provides
    TransactionLocalSource provideTransactionInDiskCache(RealmManager realmManager) {
        return new TransactionsRealmCache(realmManager);
    }

	@Singleton
	@Provides
    TransactionsNetworkClientType provideBlockExplorerClient(
			OkHttpClient httpClient,
			Gson gson,
			EthereumNetworkRepositoryType ethereumNetworkRepository) {
		return new TransactionsNetworkClient(httpClient, gson, ethereumNetworkRepository);
	}

	@Singleton
    @Provides
    TokenRepositoryType provideTokenRepository(
            OkHttpClient okHttpClient,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            WalletRepositoryType walletRepository,
            TokenExplorerClientType tokenExplorerClientType,
            TokenLocalSource tokenLocalSource,
            TransactionLocalSource inDiskCache,
            TickerService tickerService) {
	    return new TokenRepository(
	            okHttpClient,
	            ethereumNetworkRepository,
	            walletRepository,
	            tokenExplorerClientType,
                tokenLocalSource,
                inDiskCache,
                tickerService);
    }

	@Singleton
    @Provides
    TokenExplorerClientType provideTokenService(OkHttpClient okHttpClient, Gson gson) {
	    return new EthplorerTokenService(okHttpClient, gson);
    }

    @Singleton
    @Provides
    TokenLocalSource provideRealmTokenSource(RealmManager realmManager) {
	    return new TokensRealmSource(realmManager);
    }

    @Singleton
	@Provides
	GasSettingsRepositoryType provideGasSettingsRepository(EthereumNetworkRepositoryType ethereumNetworkRepository) {
		return new GasSettingsRepository(ethereumNetworkRepository);
	}
}
