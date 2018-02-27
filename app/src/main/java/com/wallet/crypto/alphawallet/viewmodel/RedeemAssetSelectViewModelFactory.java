package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.wallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import com.wallet.crypto.alphawallet.router.AssetDisplayRouter;
import com.wallet.crypto.alphawallet.router.RedeemSignatureDisplayRouter;

/**
 * Created by James on 27/02/2018.
 */

public class RedeemAssetSelectViewModelFactory implements ViewModelProvider.Factory
{
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final RedeemSignatureDisplayRouter redeemSignatureDisplayRouter;

    public RedeemAssetSelectViewModelFactory(
            FindDefaultWalletInteract findDefaultWalletInteract,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            RedeemSignatureDisplayRouter redeemSignatureDisplayRouter) {
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.redeemSignatureDisplayRouter = redeemSignatureDisplayRouter;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new RedeemAssetSelectViewModel(findDefaultWalletInteract, findDefaultNetworkInteract, redeemSignatureDisplayRouter);
    }
}