package com.alphawallet.app.di;

import com.alphawallet.app.ui.ActivityFragment;
import com.alphawallet.app.ui.AddTokenActivity;
import com.alphawallet.app.ui.AdvancedSettingsActivity;
import com.alphawallet.app.ui.AssetDisplayActivity;
import com.alphawallet.app.ui.BackupKeyActivity;
import com.alphawallet.app.ui.ConfirmationActivity;
import com.alphawallet.app.ui.DappBrowserFragment;
import com.alphawallet.app.ui.Erc20DetailActivity;
import com.alphawallet.app.ui.FunctionActivity;
import com.alphawallet.app.ui.GasSettingsActivity;
import com.alphawallet.app.ui.HelpActivity;
import com.alphawallet.app.ui.HomeActivity;
import com.alphawallet.app.ui.ImportTokenActivity;
import com.alphawallet.app.ui.ImportWalletActivity;
import com.alphawallet.app.ui.MyAddressActivity;
import com.alphawallet.app.ui.NewSettingsFragment;
import com.alphawallet.app.ui.RedeemAssetSelectActivity;
import com.alphawallet.app.ui.RedeemSignatureDisplayActivity;
import com.alphawallet.app.ui.SelectNetworkActivity;
import com.alphawallet.app.ui.SellDetailActivity;
import com.alphawallet.app.ui.SendActivity;
import com.alphawallet.app.ui.SplashActivity;
import com.alphawallet.app.ui.TokenDetailActivity;
import com.alphawallet.app.ui.TokenFunctionActivity;
import com.alphawallet.app.ui.TokenManagementActivity;
import com.alphawallet.app.ui.TokenScriptManagementActivity;
import com.alphawallet.app.ui.TransactionDetailActivity;
import com.alphawallet.app.ui.TransferTicketActivity;
import com.alphawallet.app.ui.TransferTicketDetailActivity;
import com.alphawallet.app.ui.WalletActionsActivity;
import com.alphawallet.app.ui.WalletConnectActivity;
import com.alphawallet.app.ui.WalletFragment;
import com.alphawallet.app.ui.WalletsActivity;

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
	@ContributesAndroidInjector(modules = TokenFunctionModule.class)
	abstract AssetDisplayActivity bindAssetDisplayActivity();

	@ActivityScope
	@ContributesAndroidInjector(modules = SellDetailModule.class)
	abstract SellDetailActivity bindSellDetailsActivity();

	@FragmentScope
	@ContributesAndroidInjector(modules = NewSettingsModule.class)
	abstract NewSettingsFragment bindNewSettingsFragment();

	@FragmentScope
	@ContributesAndroidInjector(modules = ActivityModule.class)
	abstract ActivityFragment bindActivityFragment();

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

	@ActivityScope
	@ContributesAndroidInjector(modules = TransferTicketDetailModule.class)
	abstract TransferTicketDetailActivity bindTransferTicketDetailActivity();

	@ActivityScope
	@ContributesAndroidInjector(modules = TransferTicketModule.class)
	abstract TransferTicketActivity bindTransferTicketActivity();

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
	@ContributesAndroidInjector(modules = BackupKeyModule.class)
	abstract BackupKeyActivity bindBackupKeyActivity();

	@ActivityScope
	@ContributesAndroidInjector(modules = MyAddressModule.class)
	abstract MyAddressActivity bindMyAddressActivity();

	@ActivityScope
	@ContributesAndroidInjector(modules = TokenFunctionModule.class)
	abstract TokenFunctionActivity bindTokenFunctionActivity();

	@ActivityScope
	@ContributesAndroidInjector(modules = TokenFunctionModule.class)
	abstract FunctionActivity bindFunctionActivity();

	@ActivityScope
	@ContributesAndroidInjector(modules = TokenFunctionModule.class)
	abstract TokenDetailActivity bindTokenDetailActivity();

	@ContributesAndroidInjector(modules = SelectNetworkModule.class)
	abstract SelectNetworkActivity bindSelectNetworkActivity();

	@ContributesAndroidInjector(modules = TokenManagementModule.class)
	abstract TokenManagementActivity bindTokenManagementActivity();

	@ContributesAndroidInjector(modules = AdvancedSettingsModule.class)
	abstract AdvancedSettingsActivity bindAdvancedSettingsActivity();

    @ActivityScope
	@ContributesAndroidInjector(modules = TokenScriptManagementModule.class)
	abstract TokenScriptManagementActivity bindTokenScriptManagementActivity();

	@ActivityScope
	@ContributesAndroidInjector(modules = WalletConnectModule.class)
	abstract WalletConnectActivity bindWalletConnectActivity();
}
