package com.wallet.crypto.trustapp.di;

import com.wallet.crypto.trustapp.ui.AddTokenActivity;
import com.wallet.crypto.trustapp.ui.ConfirmationActivity;
import com.wallet.crypto.trustapp.ui.GasSettingsActivity;
import com.wallet.crypto.trustapp.ui.ImportWalletActivity;
import com.wallet.crypto.trustapp.ui.MyAddressActivity;
import com.wallet.crypto.trustapp.ui.SendActivity;
import com.wallet.crypto.trustapp.ui.SettingsActivity;
import com.wallet.crypto.trustapp.ui.SplashActivity;
import com.wallet.crypto.trustapp.ui.TokensActivity;
import com.wallet.crypto.trustapp.ui.TransactionDetailActivity;
import com.wallet.crypto.trustapp.ui.TransactionsActivity;
import com.wallet.crypto.trustapp.ui.WalletsActivity;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class BuildersModule {
	@ActivityScope
	@ContributesAndroidInjector(modules = SplashModule.class)
	abstract SplashActivity bindSplashModule();

	@ActivityScope
	@ContributesAndroidInjector(modules = AccountsManageModule.class)
	abstract WalletsActivity bindManageWalletsModule();

	@ActivityScope
	@ContributesAndroidInjector(modules = ImportModule.class)
	abstract ImportWalletActivity bindImportWalletModule();

	@ActivityScope
	@ContributesAndroidInjector(modules = TransactionsModule.class)
	abstract TransactionsActivity bindTransactionsModule();

    @ActivityScope
    @ContributesAndroidInjector(modules = TransactionDetailModule.class)
    abstract TransactionDetailActivity bindTransactionDetailModule();

	@ActivityScope
	@ContributesAndroidInjector(modules = SettingsModule.class)
	abstract SettingsActivity bindSettingsModule();

	@ActivityScope
	@ContributesAndroidInjector(modules = SendModule.class)
	abstract SendActivity bindSendModule();

	@ActivityScope
	@ContributesAndroidInjector(modules = ConfirmationModule.class)
	abstract ConfirmationActivity bindConfirmationModule();
    @ContributesAndroidInjector
	abstract MyAddressActivity bindMyAddressModule();

	@ActivityScope
    @ContributesAndroidInjector(modules = TokensModule.class)
	abstract TokensActivity bindTokensModule();

	@ActivityScope
	@ContributesAndroidInjector(modules = GasSettingsModule.class)
	abstract GasSettingsActivity bindGasSettingsModule();

	@ActivityScope
    @ContributesAndroidInjector(modules = AddTokenModule.class)
	abstract AddTokenActivity bindAddTokenActivity();
}
