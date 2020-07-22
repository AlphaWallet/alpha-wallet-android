package com.alphawallet.app.di;

import android.content.Context;

import com.alphawallet.app.BuildConfig;
import com.alphawallet.app.repository.EthereumNetworkRepository;
import com.alphawallet.app.service.AnalyticsService;
import com.alphawallet.app.service.AnalyticsServiceType;
import com.alphawallet.app.service.NoAnalyticsService;
import com.alphawallet.app.service.TickerServiceInterface;
import com.google.gson.Gson;

import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
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
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TickerService;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.KeystoreAccountService;
import com.alphawallet.app.service.MarketQueueService;
import com.alphawallet.app.service.NotificationService;
import com.alphawallet.app.service.OpenseaService;
import com.alphawallet.app.service.RealmManager;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.service.TransactionsNetworkClient;
import com.alphawallet.app.service.TransactionsNetworkClientType;

import java.io.File;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;

import static com.alphawallet.app.service.KeystoreAccountService.KEYSTORE_FOLDER;

@Module
public class RepositoriesModule {
	@Singleton
	@Provides
	PreferenceRepositoryType providePreferenceRepository(Context context) {
		return new SharedPreferenceRepository(context);
	}

	@Singleton
	@Provides
    AccountKeystoreService provideAccountKeyStoreService(Context context, KeyService keyService) {
        File file = new File(context.getFilesDir(), KEYSTORE_FOLDER);
		return new KeystoreAccountService(file, context.getFilesDir(), keyService);
	}

	@Singleton
    @Provides
	TickerServiceInterface provideTickerService(OkHttpClient httpClient, Gson gson, Context context) {
		return new TickerService(httpClient, gson, context);
    }

	@Singleton
	@Provides
	EthereumNetworkRepositoryType provideEthereumNetworkRepository(
            PreferenceRepositoryType preferenceRepository,
            TickerServiceInterface tickerService,
			Context context) {
		return new EthereumNetworkRepository(preferenceRepository, tickerService, context);
	}

	@Singleton
	@Provides
    WalletRepositoryType provideWalletRepository(
			PreferenceRepositoryType preferenceRepositoryType,
			AccountKeystoreService accountKeystoreService,
			EthereumNetworkRepositoryType networkRepository,
			TransactionsNetworkClientType blockExplorerClient,
			WalletDataRealmSource walletDataRealmSource,
			OkHttpClient httpClient,
			KeyService keyService) {
		return new WalletRepository(
		        preferenceRepositoryType, accountKeystoreService, networkRepository, blockExplorerClient, walletDataRealmSource, httpClient, keyService);
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
			EthereumNetworkRepositoryType ethereumNetworkRepository,
			Context context) {
		return new TransactionsNetworkClient(httpClient, gson, ethereumNetworkRepository, context);
	}

	@Singleton
    @Provides
    TokenRepositoryType provideTokenRepository(
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            TokenLocalSource tokenLocalSource,
			GasService gasService,
			TokensService tokensService) {
	    return new TokenRepository(
	            ethereumNetworkRepository,
                tokenLocalSource,
				gasService,
				tokensService);
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
	TokensService provideTokensService(EthereumNetworkRepositoryType ethereumNetworkRepository, RealmManager realmManager, OkHttpClient okHttpClient) {
		return new TokensService(ethereumNetworkRepository, realmManager, okHttpClient);
	}

	@Singleton
	@Provides
	GasService provideGasService(EthereumNetworkRepositoryType ethereumNetworkRepository) {
		return new GasService(ethereumNetworkRepository);
	}

	@Singleton
	@Provides
    MarketQueueService provideMarketQueueService(Context ctx, OkHttpClient okHttpClient,
                                                 TransactionRepositoryType transactionRepository) {
		return new MarketQueueService(ctx, okHttpClient, transactionRepository);
	}

	@Singleton
	@Provides
    OpenseaService provideOpenseaService(Context ctx, TokensService tokensService) {
		return new OpenseaService(ctx, tokensService);
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
    NotificationService provideNotificationService(Context ctx) {
		return new NotificationService(ctx);
	}

	@Singleton
	@Provides
    AssetDefinitionService provideAssetDefinitionService(OkHttpClient okHttpClient, Context ctx, NotificationService notificationService, RealmManager realmManager, EthereumNetworkRepositoryType ethereumNetworkRepository, TokensService tokensService, TokenLocalSource tls, AlphaWalletService alphaService) {
		return new AssetDefinitionService(okHttpClient, ctx, notificationService, realmManager, ethereumNetworkRepository, tokensService, tls, alphaService);
	}

	@Singleton
	@Provides
	KeyService provideKeyService(Context ctx) {
		return new KeyService(ctx);
	}

	@Singleton
	@Provides
	AnalyticsServiceType provideAnalyticsService(Context ctx) {
		if (BuildConfig.USE_ANALYTICS)
		{
			return new AnalyticsService(ctx);
		}
		else
		{
			return new NoAnalyticsService(ctx);
		}
	}
}
