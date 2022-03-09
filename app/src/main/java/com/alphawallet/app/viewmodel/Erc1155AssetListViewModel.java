package com.alphawallet.app.viewmodel;

import android.content.Context;
import android.content.Intent;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.ERC1155Token;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.NFTAssetDetailActivity;
import com.alphawallet.app.ui.Erc1155AssetSelectActivity;
import com.alphawallet.app.util.Utils;

import java.math.BigInteger;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class Erc1155AssetListViewModel extends BaseViewModel {
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;

    @Inject
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

    public TokensService getTokensService() { return tokensService; }

    public Intent showAssetDetailsIntent(Context context, Wallet wallet, Token token, BigInteger tokenId)
    {
        Intent intent = new Intent(context, NFTAssetDetailActivity.class);
        intent.putExtra(C.Key.WALLET, wallet);
        intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
        intent.putExtra(C.EXTRA_ADDRESS, token.getAddress());
        intent.putExtra(C.EXTRA_TOKEN_ID, tokenId.toString());
        intent.putExtra(C.EXTRA_STATE, ERC1155Token.getNFTTokenId(tokenId).toString());
        return intent;
    }

    public Intent openSelectionModeIntent(Context context, Token token, Wallet wallet, NFTAsset asset)
    {
        Intent intent = new Intent(context, Erc1155AssetSelectActivity.class);
        intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
        intent.putExtra(C.EXTRA_ADDRESS, token.getAddress());
        intent.putExtra(C.EXTRA_TOKEN_ID, Utils.bigIntListToString(asset.getCollectionIds(), false));
        intent.putExtra(C.Key.WALLET, wallet);
        return intent;
    }
}
