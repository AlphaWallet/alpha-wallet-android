package io.awallet.crypto.alphawallet.di;

import io.awallet.crypto.alphawallet.ui.AddTokenActivity;
import io.awallet.crypto.alphawallet.ui.AssetDisplayActivity;
import io.awallet.crypto.alphawallet.ui.BrowseMarketActivity;
import io.awallet.crypto.alphawallet.ui.ConfirmationActivity;
import io.awallet.crypto.alphawallet.ui.GasSettingsActivity;
import io.awallet.crypto.alphawallet.ui.HelpActivity;
import io.awallet.crypto.alphawallet.ui.HelpFragment;
import io.awallet.crypto.alphawallet.ui.HomeActivity;
import io.awallet.crypto.alphawallet.ui.ImportTokenActivity;
import io.awallet.crypto.alphawallet.ui.ImportWalletActivity;
import io.awallet.crypto.alphawallet.ui.MarketplaceFragment;
import io.awallet.crypto.alphawallet.ui.NewSettingsFragment;
import io.awallet.crypto.alphawallet.ui.RedeemAssetSelectActivity;
import io.awallet.crypto.alphawallet.ui.RedeemSignatureDisplayActivity;
import io.awallet.crypto.alphawallet.ui.SalesOrderActivity;
import io.awallet.crypto.alphawallet.ui.MyAddressActivity;
import io.awallet.crypto.alphawallet.ui.PurchaseTicketsActivity;
import io.awallet.crypto.alphawallet.ui.SellDetailActivity;
import io.awallet.crypto.alphawallet.ui.SellTicketActivity;
import io.awallet.crypto.alphawallet.ui.SendActivity;
import io.awallet.crypto.alphawallet.ui.SettingsActivity;
import io.awallet.crypto.alphawallet.ui.SplashActivity;
import io.awallet.crypto.alphawallet.ui.TicketTransferActivity;
import io.awallet.crypto.alphawallet.ui.TokenChangeCollectionActivity;
import io.awallet.crypto.alphawallet.ui.TokensActivity;
import io.awallet.crypto.alphawallet.ui.TransactionDetailActivity;
import io.awallet.crypto.alphawallet.ui.TransactionsFragment;
import io.awallet.crypto.alphawallet.ui.TransferTicketActivity;
import io.awallet.crypto.alphawallet.ui.TransferTicketDetailActivity;
import io.awallet.crypto.alphawallet.ui.WalletFragment;
import io.awallet.crypto.alphawallet.ui.WalletsActivity;

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

	@FragmentScope
	@ContributesAndroidInjector(modules = TransactionsModule.class)
	abstract TransactionsFragment bindTransactionsFragment();

	@ActivityScope
	@ContributesAndroidInjector(modules = HelpModule.class)
	abstract HelpActivity bindHelpActivity();
}