package com.alphawallet.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;

import java.math.BigInteger;
import java.util.Map;

public class Erc1155AssetSelectViewModel extends BaseViewModel {
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;

    private MutableLiveData<Map<BigInteger, NFTAsset>> assets = new MutableLiveData<>();


    public Erc1155AssetSelectViewModel(FetchTransactionsInteract fetchTransactionsInteract,
                                       AssetDefinitionService assetDefinitionService,
                                       TokensService tokensService)
    {
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
    }

    public TokensService getTokensService()
    {
        return tokensService;
    }

    public AssetDefinitionService getAssetDefinitionService()
    {
        return this.assetDefinitionService;
    }

    public LiveData<Map<BigInteger, NFTAsset>> assets() {
        return assets;
    }

    public void getAssets(Token token)
    {
        assets.postValue(token.getTokenAssets());
    }
}
