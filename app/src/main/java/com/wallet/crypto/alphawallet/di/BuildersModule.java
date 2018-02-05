package com.wallet.crypto.alphawallet.di;

import com.wallet.crypto.alphawallet.ui.AddTokenActivity;
import com.wallet.crypto.alphawallet.ui.ConfirmationActivity;
import com.wallet.crypto.alphawallet.ui.GasSettingsActivity;
import com.wallet.crypto.alphawallet.ui.ImportWalletActivity;
import com.wallet.crypto.alphawallet.ui.MarketOrderActivity;
import com.wallet.crypto.alphawallet.ui.MyAddressActivity;
import com.wallet.crypto.alphawallet.ui.SendActivity;
import com.wallet.crypto.alphawallet.ui.SettingsActivity;
import com.wallet.crypto.alphawallet.ui.SignatureDisplayActivity;
import com.wallet.crypto.alphawallet.ui.SplashActivity;
import com.wallet.crypto.alphawallet.ui.TicketTransferActivity;
import com.wallet.crypto.alphawallet.ui.TokenChangeCollectionActivity;
import com.wallet.crypto.alphawallet.ui.TokensActivity;
import com.wallet.crypto.alphawallet.ui.TransactionDetailActivity;
import com.wallet.crypto.alphawallet.ui.TransactionsActivity;
import com.wallet.crypto.alphawallet.ui.UseTokenActivity;
import com.wallet.crypto.alphawallet.ui.WalletsActivity;

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

	@ActivityScope
	@ContributesAndroidInjector(modules = ChangeTokenModule.class)
	abstract TokenChangeCollectionActivity bindChangeTokenCollectionActivity();

	@ActivityScope
	@ContributesAndroidInjector(modules = SignatureModule.class)
	abstract SignatureDisplayActivity bindSignatureDisplayActivity();

	@ActivityScope
	@ContributesAndroidInjector(modules = UseTokenModule.class)
	abstract UseTokenActivity bindUseTokenActivity();

	@ActivityScope
	@ContributesAndroidInjector(modules = TicketTransferModule.class)
	abstract TicketTransferActivity bindTicketTransferActivity();

	@ActivityScope
	@ContributesAndroidInjector(modules = MarketOrderModule.class)
	abstract MarketOrderActivity bindMarketOrderActivity();
}