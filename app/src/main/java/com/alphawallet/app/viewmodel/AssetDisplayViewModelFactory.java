package com.alphawallet.app.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.interact.SignatureGenerateInteract;
import com.alphawallet.app.router.MyAddressRouter;
import com.alphawallet.app.router.RedeemAssetSelectRouter;
import com.alphawallet.app.router.SellTicketRouter;
import com.alphawallet.app.router.TransferTicketRouter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.OpenseaService;

/**
 * Created by James on 22/01/2018.
 */

public class AssetDisplayViewModelFactory implements ViewModelProvider.Factory {

    private final FetchTokensInteract fetchTokensInteract;
    private final GenericWalletInteract genericWalletInteract;
    private final TransferTicketRouter transferTicketRouter;
    private final RedeemAssetSelectRouter redeemAssetSelectRouter;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final SignatureGenerateInteract signatureGenerateInteract;
    private final SellTicketRouter sellTicketRouter;
    private final MyAddressRouter myAddressRouter;
    private final AssetDefinitionService assetDefinitionService;
    private final OpenseaService openseaService;

    public AssetDisplayViewModelFactory(
            FetchTokensInteract fetchTokensInteract,
            GenericWalletInteract genericWalletInteract,
            SignatureGenerateInteract signatureGenerateInteract,
            TransferTicketRouter transferTicketRouter,
            RedeemAssetSelectRouter redeemAssetSelectRouter,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            SellTicketRouter sellTicketRouter,
            MyAddressRouter myAddressRouter,
            AssetDefinitionService assetDefinitionService,
            OpenseaService openseaService) {
        this.fetchTokensInteract = fetchTokensInteract;
        this.genericWalletInteract = genericWalletInteract;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.redeemAssetSelectRouter = redeemAssetSelectRouter;
        this.signatureGenerateInteract = signatureGenerateInteract;
        this.transferTicketRouter = transferTicketRouter;
        this.sellTicketRouter = sellTicketRouter;
        this.myAddressRouter = myAddressRouter;
        this.assetDefinitionService = assetDefinitionService;
        this.openseaService = openseaService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new AssetDisplayViewModel(fetchTokensInteract, genericWalletInteract, signatureGenerateInteract, transferTicketRouter, redeemAssetSelectRouter, findDefaultNetworkInteract, sellTicketRouter, myAddressRouter, assetDefinitionService, openseaService);
    }
}
