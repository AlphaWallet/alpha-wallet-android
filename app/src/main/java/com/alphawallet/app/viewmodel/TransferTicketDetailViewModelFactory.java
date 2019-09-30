package com.alphawallet.app.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;
import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.interact.ENSInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.router.AssetDisplayRouter;
import com.alphawallet.app.router.ConfirmationRouter;
import com.alphawallet.app.router.TransferTicketDetailRouter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.KeyService;

/**
 * Created by James on 21/02/2018.
 */

public class TransferTicketDetailViewModelFactory implements ViewModelProvider.Factory {

    private GenericWalletInteract genericWalletInteract;
    private KeyService keyService;
    private CreateTransactionInteract createTransactionInteract;
    private TransferTicketDetailRouter transferTicketDetailRouter;
    private FetchTransactionsInteract fetchTransactionsInteract;
    private AssetDisplayRouter assetDisplayRouter;
    private AssetDefinitionService assetDefinitionService;
    private GasService gasService;
    private ConfirmationRouter confirmationRouter;
    private ENSInteract ensInteract;


    public TransferTicketDetailViewModelFactory(GenericWalletInteract genericWalletInteract,
                                                KeyService keyService,
                                                CreateTransactionInteract createTransactionInteract,
                                                TransferTicketDetailRouter transferTicketDetailRouter,
                                                FetchTransactionsInteract fetchTransactionsInteract,
                                                AssetDisplayRouter assetDisplayRouter,
                                                AssetDefinitionService assetDefinitionService,
                                                GasService gasService,
                                                ConfirmationRouter confirmationRouter,
                                                ENSInteract ensInteract) {
        this.genericWalletInteract = genericWalletInteract;
        this.keyService = keyService;
        this.createTransactionInteract = createTransactionInteract;
        this.transferTicketDetailRouter = transferTicketDetailRouter;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.assetDisplayRouter = assetDisplayRouter;
        this.assetDefinitionService = assetDefinitionService;
        this.gasService = gasService;
        this.confirmationRouter = confirmationRouter;
        this.ensInteract = ensInteract;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new TransferTicketDetailViewModel(genericWalletInteract, keyService, createTransactionInteract, transferTicketDetailRouter, fetchTransactionsInteract,
                                                     assetDisplayRouter, assetDefinitionService, gasService, confirmationRouter, ensInteract);
    }
}

