package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.wallet.crypto.alphawallet.entity.GasSettings;
import com.wallet.crypto.alphawallet.entity.NetworkInfo;
import com.wallet.crypto.alphawallet.entity.Ticket;
import com.wallet.crypto.alphawallet.entity.Wallet;
import com.wallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import com.wallet.crypto.alphawallet.service.MarketQueueService;

import java.math.BigInteger;

/**
 * Created by James on 23/02/2018.
 */

public class PurchaseTicketsViewModelFactory implements ViewModelProvider.Factory {

    private FindDefaultNetworkInteract findDefaultNetworkInteract;
    private FindDefaultWalletInteract findDefaultWalletInteract;
    private MarketQueueService marketQueueService;

    public PurchaseTicketsViewModelFactory(FindDefaultNetworkInteract findDefaultNetworkInteract,
                                  FindDefaultWalletInteract findDefaultWalletInteract,
                                  MarketQueueService marketQueueService) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.marketQueueService = marketQueueService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new PurchaseTicketsViewModel(findDefaultNetworkInteract, findDefaultWalletInteract, marketQueueService);
    }
}

