package com.wallet.crypto.alphawallet.di;

import com.wallet.crypto.alphawallet.App;

import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.support.AndroidSupportInjectionModule;

@Singleton
@Component(modules = {
		AndroidSupportInjectionModule.class,
		ToolsModule.class,
		RepositoriesModule.class,
		BuildersModule.class })
public interface AppComponent {

	@Component.Builder
	interface Builder {
		@BindsInstance
		Builder application(App app);
		AppComponent build();
	}
	void inject(App app);
}
