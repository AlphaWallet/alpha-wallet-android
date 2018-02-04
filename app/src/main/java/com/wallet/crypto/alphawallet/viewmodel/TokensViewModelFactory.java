package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.wallet.crypto.alphawallet.interact.FetchTokensInteract;
import com.wallet.crypto.alphawallet.router.AddTokenRouter;
import com.wallet.crypto.alphawallet.router.ChangeTokenCollectionRouter;
import com.wallet.crypto.alphawallet.router.SendTokenRouter;
import com.wallet.crypto.alphawallet.router.TransactionsRouter;
import com.wallet.crypto.alphawallet.router.UseTokenRouter;

public class TokensViewModelFactory implements ViewModelProvider.Factory {

    //private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final AddTokenRouter addTokenRouter;
    private final SendTokenRouter sendTokenRouter;
    private final UseTokenRouter useTokenRouter;
    private final TransactionsRouter transactionsRouter;
    private final ChangeTokenCollectionRouter changeTokenCollectionRouter;


    public TokensViewModelFactory(/*FindDefaultNetworkInteract findDefaultNetworkInteract,*/
                                  FetchTokensInteract fetchTokensInteract,
                                  AddTokenRouter addTokenRouter,
                                  SendTokenRouter sendTokenRouter,
                                  TransactionsRouter transactionsRouter,
                                  ChangeTokenCollectionRouter changeTokenCollectionRouter,
                                  UseTokenRouter useTokenRouter) {
        //this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.addTokenRouter = addTokenRouter;
        this.sendTokenRouter = sendTokenRouter;
        this.useTokenRouter = useTokenRouter;
        this.transactionsRouter = transactionsRouter;
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
                transactionsRouter,
                changeTokenCollectionRouter,
                useTokenRouter);
    }
}
