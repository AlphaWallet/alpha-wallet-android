package com.wallet.crypto.trustapp.di;

import android.content.Context;

import com.google.gson.Gson;
import com.wallet.crypto.trustapp.App;
import com.wallet.crypto.trustapp.repository.KSPasswordStore;
import com.wallet.crypto.trustapp.repository.PasswordStore;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;

@Module
public class ToolsModule {
	@Provides
	Context provideContext(App application) {
		return application.getApplicationContext();
	}

	@Singleton
	@Provides
	Gson provideGson() {
		return new Gson();
	}

	@Singleton
	@Provides
	OkHttpClient okHttpClient() {
		return new OkHttpClient();
	}

	@Singleton
	@Provides
	PasswordStore passwordStore(Context context) {
		return new KSPasswordStore(context);
	}
}
