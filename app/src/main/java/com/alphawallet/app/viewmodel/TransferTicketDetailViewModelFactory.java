package com.alphawallet.app.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.service.AnalyticsServiceType;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.GasService2;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.TokensService;

/**
 * Created by James on 21/02/2018.
 */

public class TransferTicketDetailViewModelFactory implements ViewModelProvider.Factory {

    private final GenericWalletInteract genericWalletInteract;
    private final KeyService keyService;
    private final CreateTransactionInteract createTransactionInteract;
    private final FetchTransactionsInteract fetchTransactionsInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final GasService2 gasService;
    private final TokensService tokensService;
    private final AnalyticsServiceType analyticsService;


    public TransferTicketDetailViewModelFactory(GenericWalletInteract genericWalletInteract,
                                                KeyService keyService,
                                                CreateTransactionInteract createTransactionInteract,
                                                FetchTransactionsInteract fetchTransactionsInteract,
                                                AssetDefinitionService assetDefinitionService,
                                                GasService2 gasService,
                                                AnalyticsServiceType analyticsService,
                                                TokensService tokensService) {
        this.genericWalletInteract = genericWalletInteract;
        this.keyService = keyService;
        this.createTransactionInteract = createTransactionInteract;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.gasService = gasService;
        this.analyticsService = analyticsService;
        this.tokensService = tokensService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new TransferTicketDetailViewModel(genericWalletInteract, keyService, createTransactionInteract, fetchTransactionsInteract,
                                                     assetDefinitionService, gasService, analyticsService, tokensService);
    }
}

