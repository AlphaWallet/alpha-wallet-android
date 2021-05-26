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
import com.alphawallet.app.ui.Erc1155AssetListActivity;
import com.alphawallet.app.ui.Erc1155AssetSelectActivity;

import java.util.HashMap;
import java.util.Map;

public class Erc1155AssetListViewModel extends BaseViewModel {
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;

    private MutableLiveData<Map<Long, ERC1155Asset>> assets = new MutableLiveData<>();

    public Erc1155AssetListViewModel(
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

    public void getAssets(Token token)
    {
        // TODO: Replace with actual data.
        // Map<Long, ERC1155Asset> data = token.getAssets();

        Map<Long, ERC1155Asset> fakeData = new HashMap<>();
        fakeData.put(1L, new ERC1155Asset("", "Sample Token", "10 Assets | Infinite FT"));
        fakeData.put(2L, new ERC1155Asset("", "Sample Token", "20 Assets | Infinite FT"));
        fakeData.put(3L, new ERC1155Asset("", "Sample Token", "30 Assets | Infinite FT"));

        assets.postValue(fakeData);
    }

    public void showAssetDetails(Context context, Wallet wallet, Token token, ERC1155Asset asset)
    {
        Intent intent = new Intent(context, Erc1155AssetDetailActivity.class);
        intent.putExtra(C.Key.WALLET, wallet);
        intent.putExtra(C.EXTRA_TOKEN_ID, token);
        intent.putExtra("asset", asset);
        context.startActivity(intent);
    }

    public void openSelectionMode(Context context, Token token, Wallet wallet)
    {
        Intent intent = new Intent(context, Erc1155AssetSelectActivity.class);
        intent.putExtra(C.EXTRA_TOKEN_ID, token);
        intent.putExtra(C.Key.WALLET, wallet);
        context.startActivity(intent);
    }
}
