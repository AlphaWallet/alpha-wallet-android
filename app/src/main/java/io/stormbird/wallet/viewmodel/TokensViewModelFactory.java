package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.router.AddTokenRouter;
import io.stormbird.wallet.router.AssetDisplayRouter;
import io.stormbird.wallet.router.ChangeTokenCollectionRouter;
import io.stormbird.wallet.router.HomeRouter;
import io.stormbird.wallet.router.SendTokenRouter;

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
