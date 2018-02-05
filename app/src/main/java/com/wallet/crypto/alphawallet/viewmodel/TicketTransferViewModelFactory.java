package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.wallet.crypto.alphawallet.interact.FetchTokensInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import com.wallet.crypto.alphawallet.interact.TicketTransferInteract;
import com.wallet.crypto.alphawallet.router.ConfirmationRouter;
import com.wallet.crypto.alphawallet.router.TicketTransferRouter;

/**
 * Created by James on 28/01/2018.
 */

public class TicketTransferViewModelFactory implements ViewModelProvider.Factory {
    private final TicketTransferInteract ticketTransferInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final TicketTransferRouter ticketTransferRouter;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final ConfirmationRouter confirmationRouter;

    public TicketTransferViewModelFactory(
            TicketTransferInteract ticketTransferInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            FetchTokensInteract fetchTokensInteract,
            TicketTransferRouter ticketTransferRouter,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            ConfirmationRouter confirmationRouter) {
        this.ticketTransferInteract = ticketTransferInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.ticketTransferRouter = ticketTransferRouter;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.confirmationRouter = confirmationRouter;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new TicketTransferViewModel(ticketTransferInteract, findDefaultWalletInteract, fetchTokensInteract, ticketTransferRouter, findDefaultNetworkInteract, confirmationRouter);
    }
}
