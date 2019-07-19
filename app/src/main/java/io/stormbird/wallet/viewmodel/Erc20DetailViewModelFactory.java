package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;

import io.reactivex.annotations.NonNull;
import io.stormbird.wallet.interact.*;
import io.stormbird.wallet.router.MyAddressRouter;
import io.stormbird.wallet.router.TransactionDetailRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.TokensService;

public class Erc20DetailViewModelFactory implements ViewModelProvider.Factory {

    private final MyAddressRouter myAddressRouter;
    private final FetchTokensInteract fetchTokensInteract;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final GenericWalletInteract genericWalletInteract;
    private final TransactionDetailRouter transactionDetailRouter;
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;
    private final AddTokenInteract addTokenInteract;

    public Erc20DetailViewModelFactory(MyAddressRouter myAddressRouter,
                                       FetchTokensInteract fetchTokensInteract,
                                       FetchTransactionsInteract fetchTransactionsInteract,
                                       FindDefaultNetworkInteract findDefaultNetworkInteract,
                                       GenericWalletInteract genericWalletInteract,
                                       TransactionDetailRouter transactionDetailRouter,
                                       AssetDefinitionService assetDefinitionService,
                                       TokensService tokensService,
                                       AddTokenInteract addTokenInteract) {
        this.myAddressRouter = myAddressRouter;
        this.fetchTokensInteract = fetchTokensInteract;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.genericWalletInteract = genericWalletInteract;
        this.transactionDetailRouter = transactionDetailRouter;
        this.assetDefinitionService = assetDefinitionService;
        this.addTokenInteract = addTokenInteract;
        this.tokensService = tokensService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new Erc20DetailViewModel(myAddressRouter, fetchTokensInteract, fetchTransactionsInteract, findDefaultNetworkInteract, genericWalletInteract, transactionDetailRouter, assetDefinitionService, tokensService, addTokenInteract);
    }
}
