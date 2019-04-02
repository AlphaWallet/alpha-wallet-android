package io.stormbird.wallet.viewmodel;

import io.stormbird.wallet.service.AssetDefinitionService;

/**
 * Created by James on 2/04/2019.
 * Stormbird in Singapore
 */
public class TokenFunctionViewModel extends BaseViewModel
{
    private final AssetDefinitionService assetDefinitionService;

    TokenFunctionViewModel(
            AssetDefinitionService assetDefinitionService) {
        this.assetDefinitionService = assetDefinitionService;
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return assetDefinitionService;
    }
}
