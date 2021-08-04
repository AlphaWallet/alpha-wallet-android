package com.alphawallet.app.viewmodel;

import android.content.Context;
import android.content.Intent;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.Erc1155AssetDetailActivity;
import com.alphawallet.app.ui.Erc1155AssetSelectActivity;

public class Erc1155AssetListViewModel extends BaseViewModel {
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;

    public Erc1155AssetListViewModel(
            AssetDefinitionService assetDefinitionService,
            TokensService tokensService)
    {
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
    }

    public AssetDefinitionService getAssetDefinitionService() {
        return assetDefinitionService;
    }

    public void getAssets(Token token)
    {

    }

    public void showAssetDetails(Context context, Wallet wallet, Token token, NFTAsset asset)
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
