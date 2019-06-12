package io.stormbird.wallet.di;

import io.stormbird.wallet.interact.CreateTransactionInteract;
import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.repository.PasswordStore;
import io.stormbird.wallet.repository.TokenRepositoryType;
import io.stormbird.wallet.repository.TransactionRepositoryType;
import io.stormbird.wallet.router.SellTicketRouter;
import io.stormbird.wallet.router.TransferTicketDetailRouter;
import io.stormbird.wallet.router.TransferTicketRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.TokensService;
import io.stormbird.wallet.viewmodel.TokenFunctionViewModelFactory;
import dagger.Module;
import dagger.Provides;
/**
 * Created by James on 2/04/2019.
 * Stormbird in Singapore
 */

@Module
public class TokenFunctionModule
{
    @Provides
    TokenFunctionViewModelFactory provideTokenFunctionViewModelFactory(
            AssetDefinitionService assetDefinitionService,
            SellTicketRouter sellTicketRouter,
            TransferTicketDetailRouter transferTicketRouter,
            CreateTransactionInteract createTransactionInteract,
            FetchTokensInteract fetchTokensInteract,
            TokensService tokensService,
            EthereumNetworkRepositoryType ethereumNetworkRepository) {

        return new TokenFunctionViewModelFactory(
                assetDefinitionService, sellTicketRouter, transferTicketRouter, createTransactionInteract, fetchTokensInteract, tokensService, ethereumNetworkRepository);
    }

    @Provides
    SellTicketRouter provideSellTicketRouter() {
        return new SellTicketRouter();
    }

    @Provides
    TransferTicketDetailRouter provideTransferTicketRouter() {
        return new TransferTicketDetailRouter();
    }

    @Provides
    CreateTransactionInteract provideCreateTransactionInteract(TransactionRepositoryType transactionRepository, PasswordStore passwordStore) {
        return new CreateTransactionInteract(transactionRepository, passwordStore);
    }

    @Provides
    FetchTokensInteract provideFetchTokensInteract(TokenRepositoryType tokenRepository) {
        return new FetchTokensInteract(tokenRepository);
    }
}
