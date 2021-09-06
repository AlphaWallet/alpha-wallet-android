package com.alphawallet.app.viewmodel;

import android.content.Context;
import android.content.Intent;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.alphawallet.app.C;
import com.alphawallet.app.entity.Wallet;
import com.alphawallet.app.entity.nftassets.NFTAsset;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.ui.Erc1155AssetSelectActivity;
import com.alphawallet.app.ui.TransferNFTActivity;
import com.alphawallet.app.util.Utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.reactivex.Single;

public class Erc1155AssetDetailViewModel extends BaseViewModel {
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;
    private final GenericWalletInteract walletInteract;

    private MutableLiveData<Map<BigInteger, NFTAsset>> assets = new MutableLiveData<>();

    public Erc1155AssetDetailViewModel(
            AssetDefinitionService assetDefinitionService,
            TokensService tokensService,
            GenericWalletInteract walletInteract)
    {
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
        this.walletInteract = walletInteract;
    }

    public LiveData<Map<BigInteger, NFTAsset>> assets()
    {
        return assets;
    }

    public AssetDefinitionService getAssetDefinitionService() {
        return assetDefinitionService;
    }

    public TokensService getTokensService() { return tokensService; }

    public Single<Intent> showTransferSelectCount(Context ctx, Token token, BigInteger tokenId)
    {
        return walletInteract.find()
                .map(wallet -> completeTransferSelect(ctx, token, tokenId, wallet));
    }

    private Intent completeTransferSelect(Context ctx, Token token, BigInteger tokenId, Wallet wallet)
    {
        Intent intent = new Intent(ctx, Erc1155AssetSelectActivity.class);
        intent.putExtra(C.Key.WALLET, wallet);
        intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
        intent.putExtra(C.EXTRA_ADDRESS, token.getAddress());
        intent.putExtra(C.EXTRA_TOKEN_ID, tokenId.toString(16));
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    public Single<Intent> getTransferIntent(Context ctx, Token token, List<BigInteger> tokenIds, ArrayList<NFTAsset> selection)
    {
        return walletInteract.find()
                .map(wallet -> completeTransferIntent(ctx, token, tokenIds, selection, wallet));
    }

    private Intent completeTransferIntent(Context ctx, Token token, List<BigInteger> tokenIds, ArrayList<NFTAsset> selection, Wallet wallet)
    {
        Intent intent = new Intent(ctx, TransferNFTActivity.class);
        intent.putExtra(C.Key.WALLET, wallet);
        intent.putExtra(C.EXTRA_CHAIN_ID, token.tokenInfo.chainId);
        intent.putExtra(C.EXTRA_ADDRESS, token.getAddress());
        intent.putExtra(C.EXTRA_TOKENID_LIST, Utils.bigIntListToString(tokenIds, false));
        intent.putParcelableArrayListExtra(C.EXTRA_NFTASSET_LIST, selection);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }
}
