package com.wallet.crypto.trustapp.di;

import android.content.Context;

import com.google.gson.Gson;
import com.wallet.crypto.trustapp.repository.AccountRepository;
import com.wallet.crypto.trustapp.repository.AccountRepositoryType;
import com.wallet.crypto.trustapp.repository.EthereumNetworkRepository;
import com.wallet.crypto.trustapp.repository.EthereumNetworkRepositoryType;
import com.wallet.crypto.trustapp.repository.PreferenceRepositoryType;
import com.wallet.crypto.trustapp.repository.SharedPreferenceRepository;
import com.wallet.crypto.trustapp.repository.TransactionInMemorySource;
import com.wallet.crypto.trustapp.repository.TransactionLocalSource;
import com.wallet.crypto.trustapp.repository.TransactionRepository;
import com.wallet.crypto.trustapp.repository.TransactionRepositoryType;
import com.wallet.crypto.trustapp.service.AccountKeystoreService;
import com.wallet.crypto.trustapp.service.BlockExplorerClient;
import com.wallet.crypto.trustapp.service.BlockExplorerClientType;
import com.wallet.crypto.trustapp.service.GethStoreAccountService;

import java.io.File;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;

@Module
class RepositoriesModule {
	@Singleton
	@Provides
	PreferenceRepositoryType providePreferenceRepository(Context context) {
		return new SharedPreferenceRepository(context);
	}

	@Singleton
	@Provides
	AccountKeystoreService provideAccountKeyStoreService(Context context) {
		File file = new File(context.getFilesDir(), "keystore");
		return new GethStoreAccountService(file);
	}

	@Singleton
	@Provides
	EthereumNetworkRepositoryType provideEthereumNetworkRepository(PreferenceRepositoryType preferenceRepository) {
		return new EthereumNetworkRepository(preferenceRepository);
	}

	@Singleton
	@Provides
	AccountRepositoryType provideAccountRepository(
			PreferenceRepositoryType preferenceRepositoryType,
			AccountKeystoreService accountKeystoreService,
			EthereumNetworkRepositoryType networkRepository) {
		return new AccountRepository(preferenceRepositoryType, accountKeystoreService, networkRepository);
	}

	@Singleton
	@Provides
	TransactionRepositoryType provideTransactionRepository(
			EthereumNetworkRepositoryType networkRepository,
			AccountKeystoreService accountKeystoreService,
			BlockExplorerClientType blockExplorerClient) {
		TransactionLocalSource inMemoryCache = new TransactionInMemorySource();
		TransactionLocalSource inDiskCache = null;
		return new TransactionRepository(
				networkRepository,
				accountKeystoreService,
				inMemoryCache,
				inDiskCache,
				blockExplorerClient);
	}

	@Singleton
	@Provides
	BlockExplorerClientType provideBlockExplorerClient(
			OkHttpClient httpClient,
			Gson gson,
			EthereumNetworkRepositoryType ethereumNetworkRepository) {
		return new BlockExplorerClient(httpClient, gson, ethereumNetworkRepository);
	}
}
