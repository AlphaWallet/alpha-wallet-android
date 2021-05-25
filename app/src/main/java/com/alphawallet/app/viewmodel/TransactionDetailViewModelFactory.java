package com.alphawallet.app.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.router.ExternalBrowserRouter;
import com.alphawallet.app.service.AnalyticsServiceType;
import com.alphawallet.app.service.GasService2;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.TokensService;

public class TransactionDetailViewModelFactory implements ViewModelProvider.Factory {

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final ExternalBrowserRouter externalBrowserRouter;
    private final TokensService tokensService;
    private final TokenRepositoryType tokenRepository;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final KeyService keyService;
    private final GasService2 gasService;
    private final CreateTransactionInteract createTransactionInteract;
    private final AnalyticsServiceType analyticsService;

    public TransactionDetailViewModelFactory(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            ExternalBrowserRouter externalBrowserRouter,
            TokenRepositoryType tokenRepository,
            TokensService tokensService,
            FetchTransactionsInteract fetchTransactionsInteract,
            KeyService keyService,
            GasService2 gasService,
            CreateTransactionInteract createTransactionInteract,
            AnalyticsServiceType analyticsService) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.externalBrowserRouter = externalBrowserRouter;
        this.tokensService = tokensService;
        this.tokenRepository = tokenRepository;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.keyService = keyService;
        this.gasService = gasService;
        this.createTransactionInteract = createTransactionInteract;
        this.analyticsService = analyticsService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new TransactionDetailViewModel(
                findDefaultNetworkInteract,
                externalBrowserRouter,
                tokenRepository,
                tokensService,
                fetchTransactionsInteract,
                keyService,
                gasService,
                createTransactionInteract,
                analyticsService);
    }
}
