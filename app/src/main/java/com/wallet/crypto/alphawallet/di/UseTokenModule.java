package com.wallet.crypto.alphawallet.di;

import com.wallet.crypto.alphawallet.interact.FetchTokensInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import com.wallet.crypto.alphawallet.interact.SignatureGenerateInteract;
import com.wallet.crypto.alphawallet.repository.EthereumNetworkRepositoryType;
import com.wallet.crypto.alphawallet.repository.TokenRepositoryType;
import com.wallet.crypto.alphawallet.repository.WalletRepositoryType;
import com.wallet.crypto.alphawallet.router.SalesOrderRouter;
import com.wallet.crypto.alphawallet.router.MyTokensRouter;
import com.wallet.crypto.alphawallet.router.SellTicketRouter;
import com.wallet.crypto.alphawallet.router.SignatureDisplayRouter;
import com.wallet.crypto.alphawallet.router.TicketTransferRouter;
import com.wallet.crypto.alphawallet.viewmodel.UseTokenViewModelFactory;

import dagger.Module;
import dagger.Provides;

/**
 * Created by James on 22/01/2018.
 */

@Module
public class UseTokenModule {
    @Provides
    UseTokenViewModelFactory useTokenViewModelFactory(
            FetchTokensInteract fetchTokensInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            SignatureGenerateInteract signatureGenerateInteract,
            MyTokensRouter myTokensRouter,
            TicketTransferRouter ticketTransferRouter,
            SignatureDisplayRouter signatureDisplayRouter,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            SalesOrderRouter salesOrderRouter,
            SellTicketRouter sellTicketRouter) {
        return new UseTokenViewModelFactory(
                fetchTokensInteract, findDefaultWalletInteract, signatureGenerateInteract, myTokensRouter, ticketTransferRouter, signatureDisplayRouter, findDefaultNetworkInteract, salesOrderRouter, sellTicketRouter);
    }

    @Provides
    FindDefaultNetworkInteract provideFindDefaultNetworkInteract(
            EthereumNetworkRepositoryType networkRepository) {
        return new FindDefaultNetworkInteract(networkRepository);
    }

    @Provides
    FetchTokensInteract providefetchTokensInteract(
            TokenRepositoryType tokenRepository) {
        return new FetchTokensInteract(tokenRepository);
    }

    @Provides
    FindDefaultWalletInteract provideFindDefaultWalletInteract(WalletRepositoryType walletRepository) {
        return new FindDefaultWalletInteract(walletRepository);
    }

    @Provides
    MyTokensRouter provideMyTokensRouter() {
        return new MyTokensRouter();
    }

    @Provides
    SalesOrderRouter provideSalesOrderRouter() {
        return new SalesOrderRouter();
    }

    @Provides
    TicketTransferRouter tiketTransferRouter() {
        return new TicketTransferRouter();
    }

    @Provides
    SignatureDisplayRouter provideSignatureDisplayRouter() {
        return new SignatureDisplayRouter();
    }

    @Provides
    SignatureGenerateInteract provideSignatureGenerateInteract(WalletRepositoryType walletRepository) {
        return new SignatureGenerateInteract(walletRepository);
    }

    @Provides
    SellTicketRouter provideSellTicketRouter() {
        return new SellTicketRouter();
    }
}
