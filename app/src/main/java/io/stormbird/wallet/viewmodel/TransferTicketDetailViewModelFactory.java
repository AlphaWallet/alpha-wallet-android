package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;
import io.stormbird.wallet.interact.*;
import io.stormbird.wallet.router.AssetDisplayRouter;
import io.stormbird.wallet.router.ConfirmationRouter;
import io.stormbird.wallet.router.TransferTicketDetailRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.MarketQueueService;
import io.stormbird.wallet.service.TokensService;

/**
 * Created by James on 21/02/2018.
 */

public class TransferTicketDetailViewModelFactory implements ViewModelProvider.Factory {

    private FindDefaultNetworkInteract findDefaultNetworkInteract;
    private FindDefaultWalletInteract findDefaultWalletInteract;
    private MarketQueueService marketQueueService;
    private CreateTransactionInteract createTransactionInteract;
    private TransferTicketDetailRouter transferTicketDetailRouter;
    private FetchTransactionsInteract fetchTransactionsInteract;
    private AssetDisplayRouter assetDisplayRouter;
    private AssetDefinitionService assetDefinitionService;
    private TokensService tokensService;
    private ConfirmationRouter confirmationRouter;
    private ENSInteract ensInteract;


    public TransferTicketDetailViewModelFactory(FindDefaultNetworkInteract findDefaultNetworkInteract,
                                                FindDefaultWalletInteract findDefaultWalletInteract,
                                                MarketQueueService marketQueueService,
                                                CreateTransactionInteract createTransactionInteract,
                                                TransferTicketDetailRouter transferTicketDetailRouter,
                                                FetchTransactionsInteract fetchTransactionsInteract,
                                                AssetDisplayRouter assetDisplayRouter,
                                                AssetDefinitionService assetDefinitionService,
                                                TokensService tokensService,
                                                ConfirmationRouter confirmationRouter,
                                                ENSInteract ensInteract) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.marketQueueService = marketQueueService;
        this.createTransactionInteract = createTransactionInteract;
        this.transferTicketDetailRouter = transferTicketDetailRouter;
        this.fetchTransactionsInteract = fetchTransactionsInteract;
        this.assetDisplayRouter = assetDisplayRouter;
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;
        this.confirmationRouter = confirmationRouter;
        this.ensInteract = ensInteract;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new TransferTicketDetailViewModel(findDefaultNetworkInteract, findDefaultWalletInteract, marketQueueService, createTransactionInteract, transferTicketDetailRouter, fetchTransactionsInteract,
                                                     assetDisplayRouter, assetDefinitionService, tokensService, confirmationRouter, ensInteract);
    }
}

