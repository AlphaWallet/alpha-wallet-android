package io.stormbird.wallet.di;

import io.stormbird.wallet.ui.*;

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

	@ActivityScope
	@ContributesAndroidInjector(modules = TokenFunctionModule.class)
	abstract TokenFunctionActivity bindTokenFunctionActivity();
}