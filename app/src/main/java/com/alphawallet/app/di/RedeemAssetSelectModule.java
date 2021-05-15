package com.alphawallet.app.di;

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
            RedeemSignatureDisplayRouter redeemSignatureDisplayRouter,
            AssetDefinitionService assetDefinitionService) {

        return new RedeemAssetSelectViewModelFactory(redeemSignatureDisplayRouter, assetDefinitionService);
    }

    @Provides
    RedeemSignatureDisplayRouter provideRedeemSignatureDisplayRouter() {
        return new RedeemSignatureDisplayRouter();
    }
}
