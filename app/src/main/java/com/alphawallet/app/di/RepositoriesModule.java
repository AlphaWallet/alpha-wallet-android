package com.alphawallet.app.di;

import android.content.Context;

import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.OnRampRepository;
import com.alphawallet.app.repository.OnRampRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.repository.SharedPreferenceRepository;
import com.alphawallet.app.repository.TokenLocalSource;
import com.alphawallet.app.repository.TokenRepository;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.repository.TokensRealmSource;
import com.alphawallet.app.repository.TransactionLocalSource;
import com.alphawallet.app.repository.TransactionRepository;
import com.alphawallet.app.repository.TransactionRepositoryType;
import com.alphawallet.app.repository.TransactionsRealmCache;
import com.alphawallet.app.repository.WalletDataRealmSource;
import com.alphawallet.app.repository.WalletRepository;
import com.alphawallet.app.repository.WalletRepositoryType;
import com.alphawallet.app.service.AccountKeystoreService;
import com.alphawallet.app.service.AlphaWalletService;
import com.alphawallet.app.service.AnalyticsService;
import com.alphawallet.app.service.AnalyticsServiceType;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.KeystoreAccountService;
import com.alphawallet.app.service.NotificationService;
import com.alphawallet.app.service.OpenSeaService;
import com.alphawallet.app.service.RealmManager;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.service.TransactionsNetworkClient;
import com.alphawallet.app.service.TransactionsNetworkClientType;
import com.alphawallet.app.service.TransactionsService;
import com.google.gson.Gson;

import java.io.File;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;

import static com.alphawallet.app.service.KeystoreAccountService.KEYSTORE_FOLDER;

@Module
@InstallIn(SingletonComponent.class)
public class RepositoriesModule {
	@Singleton
	@Provides
	PreferenceRepositoryType providePreferenceRepository(@ApplicationContext Context context) {
		return new SharedPreferenceRepository(context);
	}

	@Singleton
	@Provides
    AccountKeystoreService provideAccountKeyStoreService(@ApplicationContext Context context, KeyService keyService) {
        File file = new File(context.getFilesDir(), KEYSTORE_FOLDER);
		return new KeystoreAccountService(file, context.getFilesDir(), keyService);
	}

	@Singleton
    @Provides
	TickerService provideTickerService(OkHttpClient httpClient, PreferenceRepositoryType sharedPrefs, TokenLocalSource localSource) {
		return new TickerService(httpClient, sharedPrefs, localSource);
    }

	@Singleton
	@Provides
	EthereumNetworkRepositoryType provideEthereumNetworkRepository(
            PreferenceRepositoryType preferenceRepository,
			@ApplicationContext Context context) {
		return new EthereumNetworkRepository(preferenceRepository, context);
	}

	@Singleton
	@Provides
    WalletRepositoryType provideWalletRepository(
			PreferenceRepositoryType preferenceRepositoryType,
			AccountKeystoreService accountKeystoreService,
			EthereumNetworkRepositoryType networkRepository,
			WalletDataRealmSource walletDataRealmSource,
			KeyService keyService) {
		return new WalletRepository(
		        preferenceRepositoryType, accountKeystoreService, networkRepository, walletDataRealmSource, keyService);
	}

	@Singleton
	@Provides
	TransactionRepositoryType provideTransactionRepository(
			EthereumNetworkRepositoryType networkRepository,
			AccountKeystoreService accountKeystoreService,
            TransactionLocalSource inDiskCache,
			TransactionsService transactionsService) {
		return new TransactionRepository(
				networkRepository,
				accountKeystoreService,
				inDiskCache,
				transactionsService);
	}

	@Singleton
	@Provides
	OnRampRepositoryType provideOnRampRepository(@ApplicationContext Context context, AnalyticsServiceType analyticsServiceType) {
		return new OnRampRepository(context, analyticsServiceType);
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
			RealmManager realmManager) {
		return new TransactionsNetworkClient(httpClient, gson, realmManager);
	}

	@Singleton
    @Provides
    TokenRepositoryType provideTokenRepository(
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            TokenLocalSource tokenLocalSource,
			OkHttpClient httpClient,
			@ApplicationContext Context context,
			TickerService tickerService) {
	    return new TokenRepository(
	            ethereumNetworkRepository,
				tokenLocalSource,
				httpClient,
				context,
				tickerService);
    }

    @Singleton
    @Provides
    TokenLocalSource provideRealmTokenSource(RealmManager realmManager, EthereumNetworkRepositoryType ethereumNetworkRepository) {
	    return new TokensRealmSource(realmManager, ethereumNetworkRepository);
    }

	@Singleton
	@Provides
	WalletDataRealmSource provideRealmWalletDataSource(RealmManager realmManager) {
		return new WalletDataRealmSource(realmManager);
	}

	@Singleton
	@Provides
	TokensService provideTokensService(EthereumNetworkRepositoryType ethereumNetworkRepository,
									   TokenRepositoryType tokenRepository,
									   TickerService tickerService,
									   OpenSeaService openseaService,
									   AnalyticsServiceType analyticsService) {
		return new TokensService(ethereumNetworkRepository, tokenRepository, tickerService, openseaService, analyticsService);
	}

	@Singleton
	@Provides
	TransactionsService provideTransactionsService(TokensService tokensService,
												   EthereumNetworkRepositoryType ethereumNetworkRepositoryType,
												   TransactionsNetworkClientType transactionsNetworkClientType,
												   TransactionLocalSource transactionLocalSource) {
		return new TransactionsService(tokensService, ethereumNetworkRepositoryType, transactionsNetworkClientType, transactionLocalSource);
	}

	@Singleton
	@Provides
    GasService provideGasService2(EthereumNetworkRepositoryType ethereumNetworkRepository, OkHttpClient client, RealmManager realmManager) {
		return new GasService(ethereumNetworkRepository, client, realmManager);
	}

	@Singleton
	@Provides
	OpenSeaService provideOpenseaService() {
		return new OpenSeaService();
	}

	@Singleton
	@Provides
    AlphaWalletService provideFeemasterService(OkHttpClient okHttpClient,
                                               TransactionRepositoryType transactionRepository,
                                               Gson gson) {
		return new AlphaWalletService(okHttpClient, transactionRepository, gson);
	}

	@Singleton
	@Provides
    NotificationService provideNotificationService(@ApplicationContext Context ctx) {
		return new NotificationService(ctx);
	}

	@Singleton
	@Provides
    AssetDefinitionService provideAssetDefinitionService(OkHttpClient okHttpClient, @ApplicationContext Context ctx, NotificationService notificationService, RealmManager realmManager,
														 TokensService tokensService, TokenLocalSource tls, TransactionRepositoryType trt,
														 AlphaWalletService alphaService) {
		return new AssetDefinitionService(okHttpClient, ctx, notificationService, realmManager, tokensService, tls, trt, alphaService);
	}

	@Singleton
	@Provides
	KeyService provideKeyService(@ApplicationContext Context ctx, AnalyticsServiceType analyticsService) {
		return new KeyService(ctx, analyticsService);
	}

	@Singleton
	@Provides
	AnalyticsServiceType provideAnalyticsService(@ApplicationContext Context ctx) {
		return new AnalyticsService(ctx);
	}
}
