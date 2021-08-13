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
import com.alphawallet.app.ui.TransferNFTActivity;
import com.alphawallet.app.util.Utils;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

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

    public void showTransferToken(Context ctx, Token token, List<BigInteger> selection)
    {
        walletInteract.find()
                .subscribe(wallet -> completeTransfer(ctx, token, selection, wallet), this::onError)
                .isDisposed();
    }

    private void completeTransfer(Context ctx, Token token, List<BigInteger> selection, Wallet wallet)
    {
        Intent intent = new Intent(ctx, TransferNFTActivity.class);
        intent.putExtra(C.Key.WALLET, wallet);
        intent.putExtra(C.EXTRA_TOKEN, token);

        intent.putExtra(C.EXTRA_TOKENID_LIST, Utils.bigIntListToString(selection, false));

        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        ctx.startActivity(intent);
    }
}
