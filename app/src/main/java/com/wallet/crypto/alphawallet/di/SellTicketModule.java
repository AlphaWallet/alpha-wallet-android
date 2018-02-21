package com.wallet.crypto.alphawallet.di;

import com.wallet.crypto.alphawallet.interact.CreateTransactionInteract;
import com.wallet.crypto.alphawallet.interact.FetchTokensInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import com.wallet.crypto.alphawallet.repository.EthereumNetworkRepositoryType;
import com.wallet.crypto.alphawallet.repository.PasswordStore;
import com.wallet.crypto.alphawallet.repository.TokenRepositoryType;
import com.wallet.crypto.alphawallet.repository.TransactionRepositoryType;
import com.wallet.crypto.alphawallet.repository.WalletRepositoryType;
import com.wallet.crypto.alphawallet.router.ConfirmationRouter;
import com.wallet.crypto.alphawallet.router.SellTicketRouter;
import com.wallet.crypto.alphawallet.service.MarketQueueService;
import com.wallet.crypto.alphawallet.viewmodel.MarketOrderViewModelFactory;
import com.wallet.crypto.alphawallet.viewmodel.SellTicketModelFactory;

import dagger.Module;
import dagger.Provides;

/**
 * Created by James on 16/02/2018.
 */

@Module
public class SellTicketModule
{
    @Provides
    SellTicketModelFactory sellTicketModelFactory(
            FetchTokensInteract fetchTokensInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            ConfirmationRouter confirmationRouter) {
        return new SellTicketModelFactory(
                fetchTokensInteract, findDefaultWalletInteract, findDefaultNetworkInteract, confirmationRouter);
    }

    @Provides
    FindDefaultNetworkInteract provideFindDefaultNetworkInteract(
            EthereumNetworkRepositoryType networkRepository) {
        return new FindDefaultNetworkInteract(networkRepository);
    }

    @Provides
    FindDefaultWalletInteract provideFindDefaultWalletInteract(WalletRepositoryType walletRepository) {
        return new FindDefaultWalletInteract(walletRepository);
    }

    @Provides
    ConfirmationRouter provideConfirmationTicketRouter() {
        return new ConfirmationRouter();
    }

    @Provides
    FetchTokensInteract provideFetchTokensInteract(TokenRepositoryType tokenRepository) {
        return new FetchTokensInteract(tokenRepository);
    }
}

