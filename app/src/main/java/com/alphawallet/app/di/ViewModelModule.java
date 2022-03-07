package com.alphawallet.app.di;

import com.alphawallet.app.interact.ChangeTokenEnableInteract;
import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.interact.DeleteWalletInteract;
import com.alphawallet.app.interact.ExportWalletInteract;
import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.interact.FetchWalletsInteract;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.interact.ImportWalletInteract;
import com.alphawallet.app.interact.MemPoolInteract;
import com.alphawallet.app.interact.SetDefaultWalletInteract;
import com.alphawallet.app.interact.SignatureGenerateInteract;
import com.alphawallet.app.repository.CurrencyRepository;
import com.alphawallet.app.repository.CurrencyRepositoryType;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.LocaleRepository;
import com.alphawallet.app.repository.LocaleRepositoryType;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.repository.TransactionRepositoryType;
import com.alphawallet.app.repository.WalletRepositoryType;
import com.alphawallet.app.router.ExternalBrowserRouter;
import com.alphawallet.app.router.HomeRouter;
import com.alphawallet.app.router.ImportTokenRouter;
import com.alphawallet.app.router.ImportWalletRouter;
import com.alphawallet.app.router.ManageWalletsRouter;
import com.alphawallet.app.router.MyAddressRouter;
import com.alphawallet.app.router.RedeemSignatureDisplayRouter;
import com.alphawallet.app.router.SellDetailRouter;
import com.alphawallet.app.router.TokenDetailRouter;
import com.alphawallet.app.router.TransferTicketDetailRouter;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ViewModelComponent;

@Module
@InstallIn(ViewModelComponent.class)
/** Module for providing dependencies to viewModels.
 * All bindings of modules from BuildersModule is shifted here as they were injected in activity for ViewModelFactory but not needed in Hilt
 * */
public class ViewModelModule {

    @Provides
    FetchWalletsInteract provideFetchWalletInteract(WalletRepositoryType walletRepository) {
        return new FetchWalletsInteract(walletRepository);
    }

    @Provides
    SetDefaultWalletInteract provideSetDefaultAccountInteract(WalletRepositoryType accountRepository) {
        return new SetDefaultWalletInteract(accountRepository);
    }

    @Provides
    ImportWalletRouter provideImportAccountRouter() {
        return new ImportWalletRouter();
    }

    @Provides
    HomeRouter provideHomeRouter() {
        return new HomeRouter();
    }

    @Provides
    FindDefaultNetworkInteract provideFindDefaultNetworkInteract(
            EthereumNetworkRepositoryType networkRepository) {
        return new FindDefaultNetworkInteract(networkRepository);
    }

    @Provides
    ImportWalletInteract provideImportWalletInteract(
            WalletRepositoryType walletRepository) {
        return new ImportWalletInteract(walletRepository);
    }

    @Provides
    ExternalBrowserRouter externalBrowserRouter() {
        return new ExternalBrowserRouter();
    }

    @Provides
    FetchTransactionsInteract provideFetchTransactionsInteract(TransactionRepositoryType transactionRepository,
                                                               TokenRepositoryType tokenRepositoryType) {
        return new FetchTransactionsInteract(transactionRepository, tokenRepositoryType);
    }

    @Provides
    CreateTransactionInteract provideCreateTransactionInteract(TransactionRepositoryType transactionRepository) {
        return new CreateTransactionInteract(transactionRepository);
    }

    @Provides
    MyAddressRouter provideMyAddressRouter() {
        return new MyAddressRouter();
    }

    @Provides
    FetchTokensInteract provideFetchTokensInteract(TokenRepositoryType tokenRepository) {
        return new FetchTokensInteract(tokenRepository);
    }

    @Provides
    SignatureGenerateInteract provideSignatureGenerateInteract(WalletRepositoryType walletRepository) {
        return new SignatureGenerateInteract(walletRepository);
    }

    @Provides
    MemPoolInteract provideMemPoolInteract(TokenRepositoryType tokenRepository) {
        return new MemPoolInteract(tokenRepository);
    }

    @Provides
    TransferTicketDetailRouter provideTransferTicketRouter() {
        return new TransferTicketDetailRouter();
    }

    @Provides
    LocaleRepositoryType provideLocaleRepository(PreferenceRepositoryType preferenceRepository) {
        return new LocaleRepository(preferenceRepository);
    }

    @Provides
    CurrencyRepositoryType provideCurrencyRepository(PreferenceRepositoryType preferenceRepository) {
        return new CurrencyRepository(preferenceRepository);
    }

    @Provides
    TokenDetailRouter provideErc20DetailRouterRouter() {
        return new TokenDetailRouter();
    }

    @Provides
    GenericWalletInteract provideGenericWalletInteract(WalletRepositoryType walletRepository) {
        return new GenericWalletInteract(walletRepository);
    }

    @Provides
    ChangeTokenEnableInteract provideChangeTokenEnableInteract(TokenRepositoryType tokenRepository) {
        return new ChangeTokenEnableInteract(tokenRepository);
    }

    @Provides
    ManageWalletsRouter provideManageWalletsRouter() {
        return new ManageWalletsRouter();
    }

    @Provides
    SellDetailRouter provideSellDetailRouter() {
        return new SellDetailRouter();
    }

    @Provides
    DeleteWalletInteract provideDeleteAccountInteract(
            WalletRepositoryType accountRepository) {
        return new DeleteWalletInteract(accountRepository);
    }

    @Provides
    ExportWalletInteract provideExportWalletInteract(
            WalletRepositoryType walletRepository) {
        return new ExportWalletInteract(walletRepository);
    }

    @Provides
    ImportTokenRouter provideImportTokenRouter() {
        return new ImportTokenRouter();
    }

    @Provides
    RedeemSignatureDisplayRouter provideRedeemSignatureDisplayRouter() {
        return new RedeemSignatureDisplayRouter();
    }
}
