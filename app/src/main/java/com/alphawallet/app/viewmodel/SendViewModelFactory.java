package com.alphawallet.app.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;

import com.alphawallet.app.interact.AddTokenInteract;
import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.interact.SetupTokensInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;

import io.reactivex.annotations.NonNull;

import com.alphawallet.app.interact.ENSInteract;
import com.alphawallet.app.router.ConfirmationRouter;
import com.alphawallet.app.router.MyAddressRouter;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.TokensService;

public class SendViewModelFactory implements ViewModelProvider.Factory {

    private final ConfirmationRouter confirmationRouter;
    private final MyAddressRouter myAddressRouter;
    private final ENSInteract ensInteract;
    private final EthereumNetworkRepositoryType networkRepository;
    private final TokensService tokensService;
    private final SetupTokensInteract setupTokensInteract;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final AddTokenInteract addTokenInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final GasService gasService;

    public SendViewModelFactory(ConfirmationRouter confirmationRouter,
                                MyAddressRouter myAddressRouter,
                                ENSInteract ensInteract,
                                EthereumNetworkRepositoryType networkRepository,
                                TokensService tokensService,
                                SetupTokensInteract setupTokensInteract,
                                FetchTransactionsInteract fetchTransactionsInteract,
                                AddTokenInteract addTokenInteract,
                                FetchTokensInteract fetchTokensInteract,
                                GasService gasService) {
        this.confirmationRouter = confirmationRouter;
        this.myAddressRouter = myAddressRouter;
        this.ensInteract = ensInteract;
        this.networkRepository = networkRepository;
        this.tokensService = tokensService;
        this.setupTokensInteract = setupTokensInteract;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.addTokenInteract = addTokenInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.gasService = gasService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new SendViewModel(confirmationRouter, myAddressRouter, ensInteract, networkRepository, tokensService, setupTokensInteract, fetchTransactionsInteract, addTokenInteract, fetchTokensInteract, gasService);
    }
}
