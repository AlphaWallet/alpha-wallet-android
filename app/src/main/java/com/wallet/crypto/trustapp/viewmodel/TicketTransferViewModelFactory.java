package com.wallet.crypto.trustapp.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.wallet.crypto.trustapp.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.trustapp.interact.FindDefaultWalletInteract;
import com.wallet.crypto.trustapp.interact.SignatureGenerateInteract;
import com.wallet.crypto.trustapp.interact.TicketTransferInteract;
import com.wallet.crypto.trustapp.interact.UseTokenInteract;
import com.wallet.crypto.trustapp.router.MyTokensRouter;
import com.wallet.crypto.trustapp.router.SignatureDisplayRouter;
import com.wallet.crypto.trustapp.router.TicketTransferRouter;

/**
 * Created by James on 28/01/2018.
 */

public class TicketTransferViewModelFactory implements ViewModelProvider.Factory
        {
    private final TicketTransferInteract ticketTransferInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final TicketTransferRouter ticketTransferRouter;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;

    public TicketTransferViewModelFactory(
            TicketTransferInteract ticketTransferInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            TicketTransferRouter ticketTransferRouter,
            FindDefaultNetworkInteract findDefaultNetworkInteract) {
        this.ticketTransferInteract = ticketTransferInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.ticketTransferRouter = ticketTransferRouter;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new TicketTransferViewModel(ticketTransferInteract, findDefaultWalletInteract, ticketTransferRouter, findDefaultNetworkInteract);
    }
}
