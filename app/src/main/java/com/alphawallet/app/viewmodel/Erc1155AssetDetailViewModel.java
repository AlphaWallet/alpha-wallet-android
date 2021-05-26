package com.alphawallet.app.viewmodel;

import android.content.Context;
import android.content.Intent;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.tokens.ERC1155Asset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.Erc1155AssetDetailActivity;

import java.util.HashMap;
import java.util.Map;

public class Erc1155AssetDetailViewModel extends BaseViewModel {
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;

    private MutableLiveData<Map<Long, ERC1155Asset>> assets = new MutableLiveData<>();


    public Erc1155AssetDetailViewModel(
            AssetDefinitionService assetDefinitionService,
            TokensService tokensService)
    {
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
    }

    public LiveData<Map<Long, ERC1155Asset>> assets()
    {
        return assets;
    }

    public AssetDefinitionService getAssetDefinitionService() {
        return assetDefinitionService;
    }
}
