package io.awallet.crypto.alphawallet.di;

import android.content.Context;

import com.google.gson.Gson;
import io.awallet.crypto.alphawallet.repository.EthereumNetworkRepository;
import io.awallet.crypto.alphawallet.repository.EthereumNetworkRepositoryType;
import io.awallet.crypto.alphawallet.repository.GasSettingsRepository;
import io.awallet.crypto.alphawallet.repository.GasSettingsRepositoryType;
import io.awallet.crypto.alphawallet.repository.PasswordStore;
import io.awallet.crypto.alphawallet.repository.PreferenceRepositoryType;
import io.awallet.crypto.alphawallet.repository.SharedPreferenceRepository;
import io.awallet.crypto.alphawallet.repository.TokenLocalSource;
import io.awallet.crypto.alphawallet.repository.TokenRepository;
import io.awallet.crypto.alphawallet.repository.TokenRepositoryType;
import io.awallet.crypto.alphawallet.repository.TokensRealmSource;
import io.awallet.crypto.alphawallet.repository.TransactionLocalSource;
import io.awallet.crypto.alphawallet.repository.TransactionRepository;
import io.awallet.crypto.alphawallet.repository.TransactionRepositoryType;
import io.awallet.crypto.alphawallet.repository.TransactionsRealmCache;
import io.awallet.crypto.alphawallet.repository.WalletRepository;
import io.awallet.crypto.alphawallet.repository.WalletRepositoryType;
import io.awallet.crypto.alphawallet.service.AccountKeystoreService;
import io.awallet.crypto.alphawallet.service.CoinmarketcapTickerService;
import io.awallet.crypto.alphawallet.service.EthplorerTokenService;
import io.awallet.crypto.alphawallet.service.GethKeystoreAccountService;
import io.awallet.crypto.alphawallet.service.ImportTokenService;
import io.awallet.crypto.alphawallet.service.MarketQueueService;
import io.awallet.crypto.alphawallet.service.RealmManager;
import io.awallet.crypto.alphawallet.service.TickerService;
import io.awallet.crypto.alphawallet.service.TokenExplorerClientType;
import io.awallet.crypto.alphawallet.service.TransactionsNetworkClient;
import io.awallet.crypto.alphawallet.service.TransactionsNetworkClientType;

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
			PreferenceRepositoryType preferenceRepositoryType,
			AccountKeystoreService accountKeystoreService,
			EthereumNetworkRepositoryType networkRepository) {
		return new WalletRepository(
		        preferenceRepositoryType, accountKeystoreService, networkRepository);
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
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            WalletRepositoryType walletRepository,
            TokenExplorerClientType tokenExplorerClientType,
            TokenLocalSource tokenLocalSource,
            TransactionLocalSource inDiskCache,
            TickerService tickerService) {
	    return new TokenRepository(
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

	@Singleton
	@Provides
	MarketQueueService provideMarketQueueService(Context ctx, OkHttpClient okHttpClient,
												 TransactionRepositoryType transactionRepository,
												 PasswordStore passwordStore) {
		return new MarketQueueService(ctx, okHttpClient, transactionRepository, passwordStore);
	}

	@Singleton
	@Provides
	ImportTokenService provideImportTokenService(OkHttpClient okHttpClient,
												 TransactionRepositoryType transactionRepository,
												 PasswordStore passwordStore) {
		return new ImportTokenService(okHttpClient, transactionRepository, passwordStore);
	}
}
