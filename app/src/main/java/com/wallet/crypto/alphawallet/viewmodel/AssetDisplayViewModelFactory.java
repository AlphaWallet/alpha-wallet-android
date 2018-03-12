package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.wallet.crypto.alphawallet.interact.FetchTokensInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import com.wallet.crypto.alphawallet.interact.SignatureGenerateInteract;
import com.wallet.crypto.alphawallet.router.HomeRouter;
import com.wallet.crypto.alphawallet.router.RedeemAssetSelectRouter;
import com.wallet.crypto.alphawallet.router.SalesOrderRouter;
import com.wallet.crypto.alphawallet.router.MyTokensRouter;
import com.wallet.crypto.alphawallet.router.SellTicketRouter;
import com.wallet.crypto.alphawallet.router.RedeemSignatureDisplayRouter;
import com.wallet.crypto.alphawallet.router.TransferTicketRouter;

/**
 * Created by James on 22/01/2018.
 */

public class AssetDisplayViewModelFactory implements ViewModelProvider.Factory {

    private final FetchTokensInteract fetchTokensInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final MyTokensRouter myTokensRouter;
    private final TransferTicketRouter transferTicketRouter;
    private final RedeemAssetSelectRouter redeemAssetSelectRouter;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final SignatureGenerateInteract signatureGenerateInteract;
    private final SalesOrderRouter salesOrderRouter;
    private final SellTicketRouter sellTicketRouter;
    private final HomeRouter homeRouter;

    public AssetDisplayViewModelFactory(
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
        this.fetchTokensInteract = fetchTokensInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.myTokensRouter = myTokensRouter;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.redeemAssetSelectRouter = redeemAssetSelectRouter;
        this.signatureGenerateInteract = signatureGenerateInteract;
        this.transferTicketRouter = transferTicketRouter;
        this.salesOrderRouter = salesOrderRouter;
        this.sellTicketRouter = sellTicketRouter;
        this.homeRouter = homeRouter;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new AssetDisplayViewModel(fetchTokensInteract, findDefaultWalletInteract, signatureGenerateInteract, myTokensRouter, transferTicketRouter, redeemAssetSelectRouter, findDefaultNetworkInteract, salesOrderRouter, sellTicketRouter, homeRouter);
    }
}
