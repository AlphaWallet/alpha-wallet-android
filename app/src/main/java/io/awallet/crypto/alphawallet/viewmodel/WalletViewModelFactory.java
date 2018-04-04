package io.awallet.crypto.alphawallet.viewmodel;


import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import io.awallet.crypto.alphawallet.interact.FetchTokensInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import io.awallet.crypto.alphawallet.interact.GetDefaultWalletBalance;
import io.awallet.crypto.alphawallet.router.AddTokenRouter;
import io.awallet.crypto.alphawallet.router.AssetDisplayRouter;
import io.awallet.crypto.alphawallet.router.ChangeTokenCollectionRouter;
import io.awallet.crypto.alphawallet.router.SendTokenRouter;
import io.awallet.crypto.alphawallet.router.TransactionsRouter;

public class WalletViewModelFactory implements ViewModelProvider.Factory {
    private final FetchTokensInteract fetchTokensInteract;
    private final AddTokenRouter addTokenRouter;
    private final SendTokenRouter sendTokenRouter;
    private final AssetDisplayRouter assetDisplayRouter;
    private final TransactionsRouter transactionsRouter;
    private final ChangeTokenCollectionRouter changeTokenCollectionRouter;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final GetDefaultWalletBalance getDefaultWalletBalance;

    public WalletViewModelFactory(/*FindDefaultNetworkInteract findDefaultNetworkInteract,*/
                                  FetchTokensInteract fetchTokensInteract,
                                  AddTokenRouter addTokenRouter,
                                  SendTokenRouter sendTokenRouter,
                                  TransactionsRouter transactionsRouter,
                                  ChangeTokenCollectionRouter changeTokenCollectionRouter,
                                  AssetDisplayRouter assetDisplayRouter,
                                  FindDefaultNetworkInteract findDefaultNetworkInteract,
                                  FindDefaultWalletInteract findDefaultWalletInteract,
                                  GetDefaultWalletBalance getDefaultWalletBalance) {
        //this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.addTokenRouter = addTokenRouter;
        this.sendTokenRouter = sendTokenRouter;
        this.assetDisplayRouter = assetDisplayRouter;
        this.transactionsRouter = transactionsRouter;
        this.changeTokenCollectionRouter = changeTokenCollectionRouter;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.getDefaultWalletBalance = getDefaultWalletBalance;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new WalletViewModel(
                /*findDefaultNetworkInteract,*/
                fetchTokensInteract,
                addTokenRouter,
                sendTokenRouter,
                transactionsRouter,
                changeTokenCollectionRouter,
                assetDisplayRouter,
                findDefaultNetworkInteract,
                findDefaultWalletInteract,
                getDefaultWalletBalance);
    }
}
