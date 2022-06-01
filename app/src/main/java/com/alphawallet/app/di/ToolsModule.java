package com.alphawallet.app.di;

import android.content.Context;

import com.alphawallet.app.interact.WalletConnectInteract;
import com.alphawallet.app.service.RealmManager;
import com.alphawallet.app.walletconnect.AWWalletConnectClient;
import com.google.gson.Gson;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;

@Module
@InstallIn(SingletonComponent.class)
public class ToolsModule {

	@Singleton
	@Provides
	Gson provideGson() {
		return new Gson();
	}

	@Singleton
	@Provides
	OkHttpClient okHttpClient() {
		return new OkHttpClient.Builder()
                //.addInterceptor(new LogInterceptor())
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
				.retryOnConnectionFailure(false)
                .build();
	}

	@Singleton
    @Provides
    RealmManager provideRealmManager() {
	    return new RealmManager();
    }

    @Singleton
	@Provides
	AWWalletConnectClient provideAWWalletConnectClient(@ApplicationContext Context context, WalletConnectInteract walletConnectInteract) {
		return new AWWalletConnectClient(context, walletConnectInteract);
	}
}
