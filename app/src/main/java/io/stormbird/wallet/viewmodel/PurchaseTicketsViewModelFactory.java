package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import io.stormbird.wallet.interact.CreateTransactionInteract;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.service.MarketQueueService;

/**
 * Created by James on 23/02/2018.
 */

public class PurchaseTicketsViewModelFactory implements ViewModelProvider.Factory {

    private FindDefaultNetworkInteract findDefaultNetworkInteract;
    private FindDefaultWalletInteract findDefaultWalletInteract;
    private CreateTransactionInteract createTransactionInteract;
    private MarketQueueService marketQueueService;

    public PurchaseTicketsViewModelFactory(FindDefaultNetworkInteract findDefaultNetworkInteract,
                                           FindDefaultWalletInteract findDefaultWalletInteract,
                                           CreateTransactionInteract createTransactionInteract,
                                           MarketQueueService marketQueueService) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.createTransactionInteract = createTransactionInteract;
        this.marketQueueService = marketQueueService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new PurchaseTicketsViewModel(findDefaultNetworkInteract, findDefaultWalletInteract, createTransactionInteract, marketQueueService);
    }
}

