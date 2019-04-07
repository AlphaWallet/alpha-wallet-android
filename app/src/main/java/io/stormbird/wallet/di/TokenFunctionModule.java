package io.stormbird.wallet.di;

import io.stormbird.wallet.interact.CreateTransactionInteract;
import io.stormbird.wallet.repository.PasswordStore;
import io.stormbird.wallet.repository.TransactionRepositoryType;
import io.stormbird.wallet.router.SellTicketRouter;
import io.stormbird.wallet.router.TransferTicketRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
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
            TransferTicketRouter transferTicketRouter,
            CreateTransactionInteract createTransactionInteract) {

        return new TokenFunctionViewModelFactory(
                assetDefinitionService, sellTicketRouter, transferTicketRouter, createTransactionInteract);
    }

    @Provides
    SellTicketRouter provideSellTicketRouter() {
        return new SellTicketRouter();
    }

    @Provides
    TransferTicketRouter provideTransferTicketRouter() {
        return new TransferTicketRouter();
    }

    @Provides
    CreateTransactionInteract provideCreateTransactionInteract(TransactionRepositoryType transactionRepository, PasswordStore passwordStore) {
        return new CreateTransactionInteract(transactionRepository, passwordStore);
    }
}
