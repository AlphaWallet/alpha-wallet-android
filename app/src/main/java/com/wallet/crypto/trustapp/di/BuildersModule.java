package com.wallet.crypto.trustapp.di;

import com.wallet.crypto.trustapp.ui.ImportWalletActivity;
import com.wallet.crypto.trustapp.ui.ManageWalletsActivity;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class BuildersModule {
	@ActivityScope
	@ContributesAndroidInjector(modules = AccountsManageModule.class)
	abstract ManageWalletsActivity bindManageWalletsModule();

	@ActivityScope
	@ContributesAndroidInjector(modules = ImportModule.class)
	abstract ImportWalletActivity bindImportWalletModule();

}
