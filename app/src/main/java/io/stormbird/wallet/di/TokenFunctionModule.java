package io.stormbird.wallet.di;

import dagger.Module;
import dagger.Provides;
import io.stormbird.wallet.interact.CreateTransactionInteract;
import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.interact.GenericWalletInteract;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.repository.TokenRepositoryType;
import io.stormbird.wallet.repository.TransactionRepositoryType;
import io.stormbird.wallet.repository.WalletRepositoryType;
import io.stormbird.wallet.router.SellTicketRouter;
import io.stormbird.wallet.router.TransferTicketDetailRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.GasService;
import io.stormbird.wallet.service.KeyService;
import io.stormbird.wallet.service.TokensService;
import io.stormbird.wallet.viewmodel.TokenFunctionViewModelFactory;
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
            GasService gasService,
            TokensService tokensService,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            KeyService keyService,
            GenericWalletInteract genericWalletInteract) {

        return new TokenFunctionViewModelFactory(
                assetDefinitionService, sellTicketRouter, transferTicketRouter, createTransactionInteract, gasService, tokensService, ethereumNetworkRepository, keyService, genericWalletInteract);
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
    CreateTransactionInteract provideCreateTransactionInteract(TransactionRepositoryType transactionRepository) {
        return new CreateTransactionInteract(transactionRepository);
    }

    @Provides
    FetchTokensInteract provideFetchTokensInteract(TokenRepositoryType tokenRepository) {
        return new FetchTokensInteract(tokenRepository);
    }

    @Provides
    GenericWalletInteract provideGenericWalletInteract(WalletRepositoryType walletRepository) {
        return new GenericWalletInteract(walletRepository);
    }
}
