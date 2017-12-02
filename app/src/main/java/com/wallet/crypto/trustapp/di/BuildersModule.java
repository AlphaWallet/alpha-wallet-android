package com.wallet.crypto.trustapp.di;

import com.wallet.crypto.trustapp.ui.AccountsManageActivity;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class BuildersModule {
	@ActivityScope
	@ContributesAndroidInjector(modules = AccountsManageModule.class)
	abstract AccountsManageActivity bindAccountsManageModule();

}
