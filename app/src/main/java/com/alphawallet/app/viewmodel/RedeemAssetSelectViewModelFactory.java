package com.alphawallet.app.viewmodel;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.router.RedeemSignatureDisplayRouter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;

/**
 * Created by James on 27/02/2018.
 */

public class RedeemAssetSelectViewModelFactory implements ViewModelProvider.Factory
{
    private final RedeemSignatureDisplayRouter redeemSignatureDisplayRouter;
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;

    public RedeemAssetSelectViewModelFactory(
            RedeemSignatureDisplayRouter redeemSignatureDisplayRouter,
            AssetDefinitionService assetDefinitionService,
            TokensService tokensService) {
        this.redeemSignatureDisplayRouter = redeemSignatureDisplayRouter;
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new RedeemAssetSelectViewModel(redeemSignatureDisplayRouter, assetDefinitionService, tokensService);
    }
}