package com.wallet.crypto.alphawallet.di;

import com.wallet.crypto.alphawallet.interact.FetchTokensInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import com.wallet.crypto.alphawallet.interact.TicketTransferInteract;
import com.wallet.crypto.alphawallet.repository.EthereumNetworkRepositoryType;
import com.wallet.crypto.alphawallet.repository.TokenRepositoryType;
import com.wallet.crypto.alphawallet.repository.WalletRepositoryType;
import com.wallet.crypto.alphawallet.router.ConfirmationRouter;
import com.wallet.crypto.alphawallet.router.TicketTransferRouter;
import com.wallet.crypto.alphawallet.viewmodel.TicketTransferViewModelFactory;

import dagger.Module;
import dagger.Provides;

/**
 * Created by James on 28/01/2018.
 */

@Module
public class TicketTransferModule
{
    @Provides
    TicketTransferViewModelFactory ticketTransferViewModelFactory(
            TicketTransferInteract ticketTransferInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            FetchTokensInteract fetchTokensInteract,
            TicketTransferRouter ticketTransferRouter,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            ConfirmationRouter confirmationRouter) {
        return new TicketTransferViewModelFactory(
                ticketTransferInteract, findDefaultWalletInteract, fetchTokensInteract, ticketTransferRouter, findDefaultNetworkInteract, confirmationRouter);
    }

    @Provides
    FindDefaultNetworkInteract provideFindDefaultNetworkInteract(
            EthereumNetworkRepositoryType networkRepository) {
        return new FindDefaultNetworkInteract(networkRepository);
    }

    @Provides
    TicketTransferInteract provideTicketTransferInteract(
            TokenRepositoryType tokenRepository,
            WalletRepositoryType walletRepository) {
        return new TicketTransferInteract(walletRepository, tokenRepository);
    }

    @Provides
    FetchTokensInteract provideFetchTokensInteract(TokenRepositoryType tokenRepository) {
        return new FetchTokensInteract(tokenRepository);
    }

    @Provides
    FindDefaultWalletInteract provideFindDefaultWalletInteract(WalletRepositoryType walletRepository) {
        return new FindDefaultWalletInteract(walletRepository);
    }

    @Provides
    ConfirmationRouter provideConfirmationRouter() {
        return new ConfirmationRouter();
    }

    @Provides
    TicketTransferRouter provideTicketTransferRouter() {
        return new TicketTransferRouter();
    }
}
