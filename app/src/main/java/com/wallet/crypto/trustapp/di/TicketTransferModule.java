package com.wallet.crypto.trustapp.di;

import com.wallet.crypto.trustapp.interact.FetchGasSettingsInteract;
import com.wallet.crypto.trustapp.interact.FetchTokensInteract;
import com.wallet.crypto.trustapp.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.trustapp.interact.FindDefaultWalletInteract;
import com.wallet.crypto.trustapp.interact.SignatureGenerateInteract;
import com.wallet.crypto.trustapp.interact.TicketTransferInteract;
import com.wallet.crypto.trustapp.interact.UseTokenInteract;
import com.wallet.crypto.trustapp.repository.EthereumNetworkRepositoryType;
import com.wallet.crypto.trustapp.repository.GasSettingsRepositoryType;
import com.wallet.crypto.trustapp.repository.TokenRepositoryType;
import com.wallet.crypto.trustapp.repository.WalletRepositoryType;
import com.wallet.crypto.trustapp.router.ConfirmationRouter;
import com.wallet.crypto.trustapp.router.MyTokensRouter;
import com.wallet.crypto.trustapp.router.SignatureDisplayRouter;
import com.wallet.crypto.trustapp.router.TicketTransferRouter;
import com.wallet.crypto.trustapp.viewmodel.SendViewModelFactory;
import com.wallet.crypto.trustapp.viewmodel.TicketTransferViewModelFactory;
import com.wallet.crypto.trustapp.viewmodel.UseTokenViewModelFactory;

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
