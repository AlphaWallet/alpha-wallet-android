package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;
import io.stormbird.wallet.interact.*;
import io.stormbird.wallet.router.AssetDisplayRouter;
import io.stormbird.wallet.router.ConfirmationRouter;
import io.stormbird.wallet.router.TransferTicketDetailRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.GasService;
import io.stormbird.wallet.service.MarketQueueService;
import io.stormbird.wallet.service.TokensService;

/**
 * Created by James on 21/02/2018.
 */

public class TransferTicketDetailViewModelFactory implements ViewModelProvider.Factory {

    private FindDefaultWalletInteract findDefaultWalletInteract;
    private MarketQueueService marketQueueService;
    private CreateTransactionInteract createTransactionInteract;
    private TransferTicketDetailRouter transferTicketDetailRouter;
    private FetchTransactionsInteract fetchTransactionsInteract;
    private AssetDisplayRouter assetDisplayRouter;
    private AssetDefinitionService assetDefinitionService;
    private GasService gasService;
    private ConfirmationRouter confirmationRouter;
    private ENSInteract ensInteract;


    public TransferTicketDetailViewModelFactory(FindDefaultWalletInteract findDefaultWalletInteract,
                                                MarketQueueService marketQueueService,
                                                CreateTransactionInteract createTransactionInteract,
                                                TransferTicketDetailRouter transferTicketDetailRouter,
                                                FetchTransactionsInteract fetchTransactionsInteract,
                                                AssetDisplayRouter assetDisplayRouter,
                                                AssetDefinitionService assetDefinitionService,
                                                GasService gasService,
                                                ConfirmationRouter confirmationRouter,
                                                ENSInteract ensInteract) {
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.marketQueueService = marketQueueService;
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
        return (T) new TransferTicketDetailViewModel(findDefaultWalletInteract, marketQueueService, createTransactionInteract, transferTicketDetailRouter, fetchTransactionsInteract,
                                                     assetDisplayRouter, assetDefinitionService, gasService, confirmationRouter, ensInteract);
    }
}

