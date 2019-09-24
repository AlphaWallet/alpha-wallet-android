package com.alphawallet.app.di;

import dagger.Module;
import dagger.Provides;
import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.interact.SignatureGenerateInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.repository.WalletRepositoryType;
import com.alphawallet.app.router.MyAddressRouter;
import com.alphawallet.app.router.RedeemAssetSelectRouter;
import com.alphawallet.app.router.SellTicketRouter;
import com.alphawallet.app.router.TransferTicketRouter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.OpenseaService;
import com.alphawallet.app.viewmodel.AssetDisplayViewModelFactory;

/**
 * Created by James on 22/01/2018.
 */

@Module
public class AssetDisplayModule {
    @Provides
    AssetDisplayViewModelFactory redeemTokenViewModelFactory(
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
        return new AssetDisplayViewModelFactory(
                fetchTokensInteract, genericWalletInteract, signatureGenerateInteract, transferTicketRouter, redeemAssetSelectRouter, findDefaultNetworkInteract, sellTicketRouter, myAddressRouter, assetDefinitionService, openseaService);
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
    GenericWalletInteract provideFindDefaultWalletInteract(WalletRepositoryType walletRepository) {
        return new GenericWalletInteract(walletRepository);
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
    MyAddressRouter provideMyAddressRouter() {
        return new MyAddressRouter();
    }
}
