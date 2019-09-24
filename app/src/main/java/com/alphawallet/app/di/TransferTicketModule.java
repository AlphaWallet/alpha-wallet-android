package com.alphawallet.app.di;

import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.repository.WalletRepositoryType;
import com.alphawallet.app.router.TransferTicketDetailRouter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.viewmodel.TransferTicketViewModelFactory;

import dagger.Module;
import dagger.Provides;

/**
 * Created by James on 16/02/2018.
 */

@Module
public class TransferTicketModule
{
    @Provides
    TransferTicketViewModelFactory transferTicketViewModelFactory(
            FetchTokensInteract fetchTokensInteract,
            GenericWalletInteract genericWalletInteract,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            TransferTicketDetailRouter transferTicketDetailRouter,
            AssetDefinitionService assetDefinitionService) {
        return new TransferTicketViewModelFactory(
                fetchTokensInteract, genericWalletInteract, findDefaultNetworkInteract, transferTicketDetailRouter, assetDefinitionService);
    }

    @Provides
    FindDefaultNetworkInteract provideFindDefaultNetworkInteract(
            EthereumNetworkRepositoryType networkRepository) {
        return new FindDefaultNetworkInteract(networkRepository);
    }

    @Provides
    GenericWalletInteract provideFindDefaultWalletInteract(WalletRepositoryType walletRepository) {
        return new GenericWalletInteract(walletRepository);
    }

    @Provides
    TransferTicketDetailRouter provideTransferTicketDetailRouter() {
        return new TransferTicketDetailRouter();
    }

    @Provides
    FetchTokensInteract provideFetchTokensInteract(TokenRepositoryType tokenRepository) {
        return new FetchTokensInteract(tokenRepository);
    }
}

