package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.wallet.crypto.alphawallet.interact.CreateTransactionInteract;
import com.wallet.crypto.alphawallet.interact.FetchTokensInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import com.wallet.crypto.alphawallet.service.MarketQueueService;

/**
 * Created by James on 5/02/2018.
 */

public class SalesOrderViewModelFactory implements ViewModelProvider.Factory
{
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final CreateTransactionInteract createTransactionInteract;
    private final MarketQueueService marketQueueService;

    public SalesOrderViewModelFactory(
            FindDefaultWalletInteract findDefaultWalletInteract,
            FetchTokensInteract fetchTokensInteract,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            CreateTransactionInteract createTransactionInteract,
            MarketQueueService marketQueueService) {
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.createTransactionInteract = createTransactionInteract;
        this.marketQueueService = marketQueueService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new SalesOrderViewModel(findDefaultWalletInteract, fetchTokensInteract, findDefaultNetworkInteract, createTransactionInteract, marketQueueService);
    }
}
