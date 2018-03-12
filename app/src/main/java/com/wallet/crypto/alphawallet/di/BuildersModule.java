package com.wallet.crypto.alphawallet.di;

import com.wallet.crypto.alphawallet.ui.AddTokenActivity;
import com.wallet.crypto.alphawallet.ui.AssetDisplayActivity;
import com.wallet.crypto.alphawallet.ui.BrowseMarketActivity;
import com.wallet.crypto.alphawallet.ui.ConfirmationActivity;
import com.wallet.crypto.alphawallet.ui.GasSettingsActivity;
import com.wallet.crypto.alphawallet.ui.HelpFragment;
import com.wallet.crypto.alphawallet.ui.HomeActivity;
import com.wallet.crypto.alphawallet.ui.ImportTokenActivity;
import com.wallet.crypto.alphawallet.ui.ImportWalletActivity;
import com.wallet.crypto.alphawallet.ui.MarketplaceFragment;
import com.wallet.crypto.alphawallet.ui.NewSettingsFragment;
import com.wallet.crypto.alphawallet.ui.RedeemAssetSelectActivity;
import com.wallet.crypto.alphawallet.ui.RedeemSignatureDisplayActivity;
import com.wallet.crypto.alphawallet.ui.SalesOrderActivity;
import com.wallet.crypto.alphawallet.ui.MyAddressActivity;
import com.wallet.crypto.alphawallet.ui.PurchaseTicketsActivity;
import com.wallet.crypto.alphawallet.ui.SellDetailActivity;
import com.wallet.crypto.alphawallet.ui.SellTicketActivity;
import com.wallet.crypto.alphawallet.ui.SendActivity;
import com.wallet.crypto.alphawallet.ui.SettingsActivity;
import com.wallet.crypto.alphawallet.ui.SplashActivity;
import com.wallet.crypto.alphawallet.ui.TicketTransferActivity;
import com.wallet.crypto.alphawallet.ui.TokenChangeCollectionActivity;
import com.wallet.crypto.alphawallet.ui.TokensActivity;
import com.wallet.crypto.alphawallet.ui.TransactionDetailActivity;
import com.wallet.crypto.alphawallet.ui.TransactionsActivity;
import com.wallet.crypto.alphawallet.ui.TransferTicketActivity;
import com.wallet.crypto.alphawallet.ui.TransferTicketDetailActivity;
import com.wallet.crypto.alphawallet.ui.WalletFragment;
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
	@ContributesAndroidInjector(modules = RedeemSignatureDisplayModule.class)
	abstract RedeemSignatureDisplayActivity bindSignatureDisplayActivity();

	@ActivityScope
	@ContributesAndroidInjector(modules = AssetDisplayModule.class)
	abstract AssetDisplayActivity bindAssetDisplayActivity();

	@ActivityScope
	@ContributesAndroidInjector(modules = TicketTransferModule.class)
	abstract TicketTransferActivity bindTicketTransferActivity();

	@ActivityScope
	@ContributesAndroidInjector(modules = SalesOrderModule.class)
	abstract SalesOrderActivity bindSalesOrderActivity();

	@ActivityScope
	@ContributesAndroidInjector(modules = SellTicketModule.class)
	abstract SellTicketActivity bindSellTicketActivity();

	@ActivityScope
	@ContributesAndroidInjector(modules = MarketBrowseModule.class)
	abstract BrowseMarketActivity bindMarketBrowseActivity();

	@ActivityScope
	@ContributesAndroidInjector(modules = SellDetailModule.class)
	abstract SellDetailActivity bindSellDetailsActivity();

	@ActivityScope
	@ContributesAndroidInjector(modules = PurchaseTicketsModule.class)
	abstract PurchaseTicketsActivity bindPurchaseTicketsActivity();

	@FragmentScope
	@ContributesAndroidInjector(modules = MarketplaceModule.class)
	abstract MarketplaceFragment bindMarketplaceFragment();

	@FragmentScope
	@ContributesAndroidInjector(modules = NewSettingsModule.class)
	abstract NewSettingsFragment bindNewSettingsFragment();

	@ActivityScope
	@ContributesAndroidInjector(modules = RedeemAssetSelectModule.class)
	abstract RedeemAssetSelectActivity bindRedeemTokenSelectActivity();

	@FragmentScope
	@ContributesAndroidInjector(modules = WalletModule.class)
	abstract WalletFragment bindWalletFragment();

	@ActivityScope
	@ContributesAndroidInjector(modules = HomeModule.class)
	abstract HomeActivity bindHomeActivity();

	@ActivityScope
	@ContributesAndroidInjector(modules = ImportTokenModule.class)
	abstract ImportTokenActivity bindImportTokenActivity();

	@FragmentScope
	@ContributesAndroidInjector(modules = HelpModule.class)
	abstract HelpFragment bindHelpFragment();

	@ActivityScope
	@ContributesAndroidInjector(modules = TransferTicketDetailModule.class)
	abstract TransferTicketDetailActivity bindTransferTicketDetailActivity();

	@ActivityScope
	@ContributesAndroidInjector(modules = TransferTicketModule.class)
	abstract TransferTicketActivity bindTransferTicketActivity();
}