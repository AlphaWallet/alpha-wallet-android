package io.stormbird.wallet.di;

import io.stormbird.wallet.ui.AddTokenActivity;
import io.stormbird.wallet.ui.AssetDisplayActivity;
import io.stormbird.wallet.ui.BrowseMarketActivity;
import io.stormbird.wallet.ui.ConfirmationActivity;
import io.stormbird.wallet.ui.Erc20DetailActivity;
import io.stormbird.wallet.ui.GasSettingsActivity;
import io.stormbird.wallet.ui.HelpActivity;
import io.stormbird.wallet.ui.HelpFragment;
import io.stormbird.wallet.ui.HomeActivity;
import io.stormbird.wallet.ui.ImportTokenActivity;
import io.stormbird.wallet.ui.ImportWalletActivity;
import io.stormbird.wallet.ui.MarketplaceFragment;
import io.stormbird.wallet.ui.NewSettingsFragment;
import io.stormbird.wallet.ui.RedeemAssetSelectActivity;
import io.stormbird.wallet.ui.RedeemSignatureDisplayActivity;
import io.stormbird.wallet.ui.MyAddressActivity;
import io.stormbird.wallet.ui.PurchaseTicketsActivity;
import io.stormbird.wallet.ui.SellDetailActivity;
import io.stormbird.wallet.ui.SellTicketActivity;
import io.stormbird.wallet.ui.SendActivity;
import io.stormbird.wallet.ui.SplashActivity;
import io.stormbird.wallet.ui.TransactionDetailActivity;
import io.stormbird.wallet.ui.TransactionsFragment;
import io.stormbird.wallet.ui.TransferTicketActivity;
import io.stormbird.wallet.ui.TransferTicketDetailActivity;
import io.stormbird.wallet.ui.WalletActionsActivity;
import io.stormbird.wallet.ui.WalletFragment;
import io.stormbird.wallet.ui.WalletsActivity;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import io.stormbird.wallet.ui.DappBrowserFragment;

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
	@ContributesAndroidInjector(modules = SendModule.class)
	abstract SendActivity bindSendModule();

	@ActivityScope
	@ContributesAndroidInjector(modules = ConfirmationModule.class)
	abstract ConfirmationActivity bindConfirmationModule();

	@ActivityScope
	@ContributesAndroidInjector(modules = GasSettingsModule.class)
	abstract GasSettingsActivity bindGasSettingsModule();

	@ActivityScope
	@ContributesAndroidInjector(modules = AddTokenModule.class)
	abstract AddTokenActivity bindAddTokenActivity();

	@ActivityScope
	@ContributesAndroidInjector(modules = RedeemSignatureDisplayModule.class)
	abstract RedeemSignatureDisplayActivity bindSignatureDisplayActivity();

	@ActivityScope
	@ContributesAndroidInjector(modules = AssetDisplayModule.class)
	abstract AssetDisplayActivity bindAssetDisplayActivity();

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

	@FragmentScope
	@ContributesAndroidInjector(modules = DappBrowserModule.class)
	abstract DappBrowserFragment bindDappBrowserFragment();

	@ActivityScope
	@ContributesAndroidInjector(modules = Erc20DetailModule.class)
	abstract Erc20DetailActivity bindErc20DetailActivity();

	@ActivityScope
	@ContributesAndroidInjector(modules = WalletActionsModule.class)
	abstract WalletActionsActivity bindWalletActionsActivity();

	@ActivityScope
	@ContributesAndroidInjector(modules = MyAddressModule.class)
	abstract MyAddressActivity bindMyAddressActivity();
}