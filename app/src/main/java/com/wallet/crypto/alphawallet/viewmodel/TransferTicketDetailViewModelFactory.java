package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.wallet.crypto.alphawallet.interact.CreateTransactionInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import com.wallet.crypto.alphawallet.service.MarketQueueService;

/**
 * Created by James on 21/02/2018.
 */

public class TransferTicketDetailViewModelFactory implements ViewModelProvider.Factory {

    private FindDefaultNetworkInteract findDefaultNetworkInteract;
    private FindDefaultWalletInteract findDefaultWalletInteract;
    private MarketQueueService marketQueueService;
    private CreateTransactionInteract createTransactionInteract;

    public TransferTicketDetailViewModelFactory(FindDefaultNetworkInteract findDefaultNetworkInteract,
                                                FindDefaultWalletInteract findDefaultWalletInteract,
                                                MarketQueueService marketQueueService,
                                                CreateTransactionInteract createTransactionInteract) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.marketQueueService = marketQueueService;
        this.createTransactionInteract = createTransactionInteract;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new TransferTicketDetailViewModel(findDefaultNetworkInteract, findDefaultWalletInteract, marketQueueService, createTransactionInteract);
    }
}

