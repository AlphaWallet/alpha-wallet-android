package io.awallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import io.awallet.crypto.alphawallet.interact.CreateTransactionInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import io.awallet.crypto.alphawallet.router.AssetDisplayRouter;
import io.awallet.crypto.alphawallet.router.TransferTicketDetailRouter;
import io.awallet.crypto.alphawallet.service.FeeMasterService;
import io.awallet.crypto.alphawallet.service.MarketQueueService;

/**
 * Created by James on 21/02/2018.
 */

public class TransferTicketDetailViewModelFactory implements ViewModelProvider.Factory {

    private FindDefaultNetworkInteract findDefaultNetworkInteract;
    private FindDefaultWalletInteract findDefaultWalletInteract;
    private MarketQueueService marketQueueService;
    private CreateTransactionInteract createTransactionInteract;
    private TransferTicketDetailRouter transferTicketDetailRouter;
    private FeeMasterService feeMasterService;
    private AssetDisplayRouter assetDisplayRouter;

    public TransferTicketDetailViewModelFactory(FindDefaultNetworkInteract findDefaultNetworkInteract,
                                                FindDefaultWalletInteract findDefaultWalletInteract,
                                                MarketQueueService marketQueueService,
                                                CreateTransactionInteract createTransactionInteract,
                                                TransferTicketDetailRouter transferTicketDetailRouter,
                                                FeeMasterService feeMasterService,
                                                AssetDisplayRouter assetDisplayRouter) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.marketQueueService = marketQueueService;
        this.createTransactionInteract = createTransactionInteract;
        this.transferTicketDetailRouter = transferTicketDetailRouter;
        this.feeMasterService = feeMasterService;
        this.assetDisplayRouter = assetDisplayRouter;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new TransferTicketDetailViewModel(findDefaultNetworkInteract, findDefaultWalletInteract, marketQueueService, createTransactionInteract, transferTicketDetailRouter, feeMasterService, assetDisplayRouter);
    }
}

