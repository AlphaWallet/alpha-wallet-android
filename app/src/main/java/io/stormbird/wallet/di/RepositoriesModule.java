package io.stormbird.wallet.di;

import android.content.Context;

import com.google.gson.Gson;

import io.stormbird.wallet.repository.EthereumNetworkRepository;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.repository.GasSettingsRepository;
import io.stormbird.wallet.repository.GasSettingsRepositoryType;
import io.stormbird.wallet.repository.PasswordStore;
import io.stormbird.wallet.repository.PreferenceRepositoryType;
import io.stormbird.wallet.repository.SharedPreferenceRepository;
import io.stormbird.wallet.repository.TokenLocalSource;
import io.stormbird.wallet.repository.TokenRepository;
import io.stormbird.wallet.repository.TokenRepositoryType;
import io.stormbird.wallet.repository.TokensRealmSource;
import io.stormbird.wallet.repository.TransactionLocalSource;
import io.stormbird.wallet.repository.TransactionRepository;
import io.stormbird.wallet.repository.TransactionRepositoryType;
import io.stormbird.wallet.repository.TransactionsRealmCache;
import io.stormbird.wallet.repository.WalletRepository;
import io.stormbird.wallet.repository.WalletRepositoryType;
import io.stormbird.wallet.service.AccountKeystoreService;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.CoinmarketcapTickerService;
import io.stormbird.wallet.service.EthplorerTokenService;
import io.stormbird.wallet.service.FeeMasterService;
import io.stormbird.wallet.service.GethKeystoreAccountService;
import io.stormbird.wallet.service.ImportTokenService;
import io.stormbird.wallet.service.MarketQueueService;
import io.stormbird.wallet.service.RealmManager;
import io.stormbird.wallet.service.TickerService;
import io.stormbird.wallet.service.TokenExplorerClientType;
import io.stormbird.wallet.service.TransactionsNetworkClient;
import io.stormbird.wallet.service.TransactionsNetworkClientType;

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
	FeeMasterService provideFeemasterService(OkHttpClient okHttpClient,
											 TransactionRepositoryType transactionRepository,
											 PasswordStore passwordStore) {
		return new FeeMasterService(okHttpClient, transactionRepository, passwordStore);
	}

	@Singleton
	@Provides
	AssetDefinitionService provideAssetDefinitionService(Context ctx) {
		return new AssetDefinitionService(ctx);
	}

	@Singleton
	@Provides
	ImportTokenService provideImportTokenService(OkHttpClient okHttpClient,
												 TransactionRepositoryType transactionRepository,
												 PasswordStore passwordStore) {
		return new ImportTokenService(okHttpClient, transactionRepository, passwordStore);
	}
}
