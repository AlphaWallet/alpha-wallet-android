package com.alphawallet.app.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;

import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.router.MyAddressRouter;
import com.alphawallet.app.router.TransactionDetailRouter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.TokensService;

import io.reactivex.annotations.NonNull;

public class Erc20DetailViewModelFactory implements ViewModelProvider.Factory {

    private final MyAddressRouter myAddressRouter;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final GenericWalletInteract genericWalletInteract;
    private final TransactionDetailRouter transactionDetailRouter;
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;
    private final FetchTokensInteract fetchTokensInteract;

    public Erc20DetailViewModelFactory(MyAddressRouter myAddressRouter,
                                       FetchTransactionsInteract fetchTransactionsInteract,
                                       FindDefaultNetworkInteract findDefaultNetworkInteract,
                                       GenericWalletInteract genericWalletInteract,
                                       TransactionDetailRouter transactionDetailRouter,
                                       AssetDefinitionService assetDefinitionService,
                                       TokensService tokensService,
                                       FetchTokensInteract fetchTokensInteract) {
        this.myAddressRouter = myAddressRouter;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.genericWalletInteract = genericWalletInteract;
        this.transactionDetailRouter = transactionDetailRouter;
        this.assetDefinitionService = assetDefinitionService;
        this.fetchTokensInteract = fetchTokensInteract;
        this.tokensService = tokensService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new Erc20DetailViewModel(myAddressRouter, fetchTransactionsInteract, findDefaultNetworkInteract, genericWalletInteract, transactionDetailRouter, assetDefinitionService, tokensService, fetchTokensInteract);
    }
}
