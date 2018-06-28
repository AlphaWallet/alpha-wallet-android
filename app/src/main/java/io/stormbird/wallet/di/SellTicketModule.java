package io.stormbird.wallet.di;

import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.repository.TokenRepositoryType;
import io.stormbird.wallet.repository.WalletRepositoryType;
import io.stormbird.wallet.router.SellDetailRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.viewmodel.SellTicketModelFactory;

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
            SellDetailRouter sellDetailRouter,
            AssetDefinitionService assetDefinitionService) {
        return new SellTicketModelFactory(
                fetchTokensInteract, findDefaultWalletInteract, findDefaultNetworkInteract, sellDetailRouter, assetDefinitionService);
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
    SellDetailRouter provideSellDetailRouter() {
        return new SellDetailRouter();
    }

    @Provides
    FetchTokensInteract provideFetchTokensInteract(TokenRepositoryType tokenRepository) {
        return new FetchTokensInteract(tokenRepository);
    }
}

