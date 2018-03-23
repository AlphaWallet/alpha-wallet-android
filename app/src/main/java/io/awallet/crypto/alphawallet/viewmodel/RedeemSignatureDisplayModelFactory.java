package io.awallet.crypto.alphawallet.viewmodel;

/**
 * Created by James on 25/01/2018.
 */

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import io.awallet.crypto.alphawallet.interact.CreateTransactionInteract;
import io.awallet.crypto.alphawallet.interact.FetchTokensInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import io.awallet.crypto.alphawallet.interact.MemPoolInteract;
import io.awallet.crypto.alphawallet.interact.SignatureGenerateInteract;
import io.awallet.crypto.alphawallet.router.AssetDisplayRouter;

/**
 * Created by James on 22/01/2018.
 */

public class RedeemSignatureDisplayModelFactory implements ViewModelProvider.Factory {

    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final SignatureGenerateInteract signatureGenerateInteract;
    private final CreateTransactionInteract createTransactionInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final MemPoolInteract memPoolInteract;
    private final AssetDisplayRouter assetDisplayRouter;

    public RedeemSignatureDisplayModelFactory(
            FindDefaultWalletInteract findDefaultWalletInteract,
            SignatureGenerateInteract signatureGenerateInteract,
            CreateTransactionInteract createTransactionInteract,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            FetchTokensInteract fetchTokensInteract,
            MemPoolInteract memPoolInteract,
            AssetDisplayRouter assetDisplayRouter) {
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.signatureGenerateInteract = signatureGenerateInteract;
        this.createTransactionInteract = createTransactionInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.memPoolInteract = memPoolInteract;
        this.assetDisplayRouter = assetDisplayRouter;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new RedeemSignatureDisplayModel(findDefaultWalletInteract, signatureGenerateInteract, createTransactionInteract, findDefaultNetworkInteract, fetchTokensInteract, memPoolInteract, assetDisplayRouter);
    }
}
