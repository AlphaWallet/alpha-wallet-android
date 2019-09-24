package com.alphawallet.app.di;

import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.WalletRepositoryType;
import com.alphawallet.app.router.RedeemSignatureDisplayRouter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.viewmodel.RedeemAssetSelectViewModelFactory;

import dagger.Module;
import dagger.Provides;

/**
 * Created by James on 27/02/2018.
 */

@Module
public class RedeemAssetSelectModule
{
    @Provides
    RedeemAssetSelectViewModelFactory redeemTokenSelectViewModelFactory(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            GenericWalletInteract genericWalletInteract,
            RedeemSignatureDisplayRouter redeemSignatureDisplayRouter,
            AssetDefinitionService assetDefinitionService) {

        return new RedeemAssetSelectViewModelFactory(
                genericWalletInteract, findDefaultNetworkInteract, redeemSignatureDisplayRouter, assetDefinitionService);
    }

    @Provides
    FindDefaultNetworkInteract provideFindDefaultNetworkInteract(
            EthereumNetworkRepositoryType networkRepository) {
        return new FindDefaultNetworkInteract(networkRepository);
    }

    @Provides
    GenericWalletInteract provideFindDefaultWalletInteract(WalletRepositoryType walletRepository) {
        return new GenericWalletInteract(walletRepository);
    }

    @Provides
    RedeemSignatureDisplayRouter provideRedeemSignatureDisplayRouter() {
        return new RedeemSignatureDisplayRouter();
    }
}
