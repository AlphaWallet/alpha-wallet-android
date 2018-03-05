package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.wallet.crypto.alphawallet.interact.FetchTokensInteract;
import com.wallet.crypto.alphawallet.router.AddTokenRouter;
import com.wallet.crypto.alphawallet.router.AssetDisplayRouter;
import com.wallet.crypto.alphawallet.router.ChangeTokenCollectionRouter;
import com.wallet.crypto.alphawallet.router.HomeRouter;
import com.wallet.crypto.alphawallet.router.SendTokenRouter;
import com.wallet.crypto.alphawallet.router.TransactionsRouter;

public class TokensViewModelFactory implements ViewModelProvider.Factory {

    //private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final AddTokenRouter addTokenRouter;
    private final SendTokenRouter sendTokenRouter;
    private final AssetDisplayRouter assetDisplayRouter;
    private final HomeRouter homeRouter;
    private final ChangeTokenCollectionRouter changeTokenCollectionRouter;


    public TokensViewModelFactory(/*FindDefaultNetworkInteract findDefaultNetworkInteract,*/
                                  FetchTokensInteract fetchTokensInteract,
                                  AddTokenRouter addTokenRouter,
                                  SendTokenRouter sendTokenRouter,
                                  HomeRouter homeRouter,
                                  ChangeTokenCollectionRouter changeTokenCollectionRouter,
                                  AssetDisplayRouter assetDisplayRouter) {
        //this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.addTokenRouter = addTokenRouter;
        this.sendTokenRouter = sendTokenRouter;
        this.assetDisplayRouter = assetDisplayRouter;
        this.homeRouter = homeRouter;
        this.changeTokenCollectionRouter = changeTokenCollectionRouter;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new TokensViewModel(
                /*findDefaultNetworkInteract,*/
                fetchTokensInteract,
                addTokenRouter,
                sendTokenRouter,
                homeRouter,
                changeTokenCollectionRouter,
                assetDisplayRouter);
    }
}
