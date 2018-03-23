package io.awallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import io.awallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import io.awallet.crypto.alphawallet.router.AssetDisplayRouter;
import io.awallet.crypto.alphawallet.router.RedeemSignatureDisplayRouter;

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