package com.alphawallet.app.di;

import android.content.Context;

import com.google.gson.Gson;
import com.alphawallet.app.App;
import com.alphawallet.app.service.RealmManager;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
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
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
				.retryOnConnectionFailure(false)
                .build();
	}

	@Singleton
    @Provides
    RealmManager provideRealmManager() {
	    return new RealmManager();
    }
}
