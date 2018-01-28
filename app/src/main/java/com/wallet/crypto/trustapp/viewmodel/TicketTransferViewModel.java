package com.wallet.crypto.trustapp.viewmodel;

import android.arch.lifecycle.MutableLiveData;

import com.wallet.crypto.trustapp.entity.NetworkInfo;
import com.wallet.crypto.trustapp.entity.Wallet;
import com.wallet.crypto.trustapp.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.trustapp.interact.FindDefaultWalletInteract;
import com.wallet.crypto.trustapp.interact.TicketTransferInteract;
import com.wallet.crypto.trustapp.router.TicketTransferRouter;

/**
 * Created by James on 28/01/2018.
 */

public class TicketTransferViewModel extends BaseViewModel
{
    private final TicketTransferInteract ticketTransferInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final TicketTransferRouter ticketTransferRouter;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;

    private final MutableLiveData<NetworkInfo> defaultNetwork = new MutableLiveData<>();
    private final MutableLiveData<Wallet> defaultWallet = new MutableLiveData<>();

    public TicketTransferViewModel(
            TicketTransferInteract ticketTransferInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            TicketTransferRouter ticketTransferRouter,
            FindDefaultNetworkInteract findDefaultNetworkInteract) {
        this.ticketTransferInteract = ticketTransferInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.ticketTransferRouter = ticketTransferRouter;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
    }

    public void prepare() {
        progress.postValue(true);
        disposable = findDefaultNetworkInteract
                .find()
                .subscribe(this::onDefaultNetwork, this::onError);
    }

    private void onDefaultNetwork(NetworkInfo networkInfo) {
        defaultNetwork.postValue(networkInfo);
        disposable = findDefaultWalletInteract
                .find()
                .subscribe(this::onDefaultWallet, this::onError);
    }

    private void onDefaultWallet(Wallet wallet) {
        //TODO: switch on 'use' button
        progress.postValue(false);
        defaultWallet.setValue(wallet);
    }

    //get tokens
}
