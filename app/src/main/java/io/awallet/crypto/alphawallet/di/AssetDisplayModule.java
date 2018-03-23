package io.awallet.crypto.alphawallet.di;

import io.awallet.crypto.alphawallet.interact.FetchTokensInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import io.awallet.crypto.alphawallet.interact.SignatureGenerateInteract;
import io.awallet.crypto.alphawallet.repository.EthereumNetworkRepositoryType;
import io.awallet.crypto.alphawallet.repository.TokenRepositoryType;
import io.awallet.crypto.alphawallet.repository.WalletRepositoryType;
import io.awallet.crypto.alphawallet.router.HomeRouter;
import io.awallet.crypto.alphawallet.router.RedeemAssetSelectRouter;
import io.awallet.crypto.alphawallet.router.SalesOrderRouter;
import io.awallet.crypto.alphawallet.router.MyTokensRouter;
import io.awallet.crypto.alphawallet.router.SellTicketRouter;
import io.awallet.crypto.alphawallet.router.TransferTicketRouter;
import io.awallet.crypto.alphawallet.viewmodel.AssetDisplayViewModelFactory;

import dagger.Module;
import dagger.Provides;

/**
 * Created by James on 22/01/2018.
 */

@Module
public class AssetDisplayModule {
    @Provides
    AssetDisplayViewModelFactory redeemTokenViewModelFactory(
            FetchTokensInteract fetchTokensInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            SignatureGenerateInteract signatureGenerateInteract,
            MyTokensRouter myTokensRouter,
            TransferTicketRouter transferTicketRouter,
            RedeemAssetSelectRouter redeemAssetSelectRouter,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            SalesOrderRouter salesOrderRouter,
            SellTicketRouter sellTicketRouter,
            HomeRouter homeRouter) {
        return new AssetDisplayViewModelFactory(
                fetchTokensInteract, findDefaultWalletInteract, signatureGenerateInteract, myTokensRouter, transferTicketRouter, redeemAssetSelectRouter, findDefaultNetworkInteract, salesOrderRouter, sellTicketRouter, homeRouter);
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
    TransferTicketRouter provideTransferTicketRouter() {
        return new TransferTicketRouter();
    }

    @Provides
    RedeemAssetSelectRouter provideRedeemAssetRouter() {
        return new RedeemAssetSelectRouter();
    }

    @Provides
    SignatureGenerateInteract provideSignatureGenerateInteract(WalletRepositoryType walletRepository) {
        return new SignatureGenerateInteract(walletRepository);
    }

    @Provides
    SellTicketRouter provideSellTicketRouter() {
        return new SellTicketRouter();
    }

    @Provides
    HomeRouter provideHomeRouter() {
        return new HomeRouter();
    }
}
