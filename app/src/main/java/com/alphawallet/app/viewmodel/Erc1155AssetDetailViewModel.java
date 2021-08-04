package com.alphawallet.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;

import java.math.BigInteger;
import java.util.Map;

public class Erc1155AssetDetailViewModel extends BaseViewModel {
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;

    private MutableLiveData<Map<BigInteger, NFTAsset>> assets = new MutableLiveData<>();


    public Erc1155AssetDetailViewModel(
            AssetDefinitionService assetDefinitionService,
            TokensService tokensService)
    {
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
    }

    public LiveData<Map<BigInteger, NFTAsset>> assets()
    {
        return assets;
    }

    public AssetDefinitionService getAssetDefinitionService() {
        return assetDefinitionService;
    }
}
