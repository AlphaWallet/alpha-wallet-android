package com.wallet.crypto.trustapp.di;

import com.wallet.crypto.trustapp.ui.ManageAccountsActivity;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class BuildersModule {
	@ActivityScope
	@ContributesAndroidInjector(modules = AccountsManageModule.class)
	abstract ManageAccountsActivity bindAccountsManageModule();

}
