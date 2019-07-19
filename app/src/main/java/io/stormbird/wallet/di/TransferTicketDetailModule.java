package io.stormbird.wallet.di;

import io.stormbird.wallet.interact.*;
import io.stormbird.wallet.repository.*;
import io.stormbird.wallet.router.AssetDisplayRouter;
import io.stormbird.wallet.router.ConfirmationRouter;
import io.stormbird.wallet.router.TransferTicketDetailRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.GasService;
import io.stormbird.wallet.service.MarketQueueService;
import io.stormbird.wallet.viewmodel.TransferTicketDetailViewModelFactory;

import dagger.Module;
import dagger.Provides;

/**
 * Created by James on 22/02/2018.
 */

@Module
public class TransferTicketDetailModule {

    @Provides
    TransferTicketDetailViewModelFactory transferTicketDetailViewModelFactory(
            GenericWalletInteract genericWalletInteract,
            MarketQueueService marketQueueService,
            CreateTransactionInteract createTransactionInteract,
            TransferTicketDetailRouter transferTicketDetailRouter,
            FetchTransactionsInteract fetchTransactionsInteract,
            AssetDisplayRouter assetDisplayRouter,
            AssetDefinitionService assetDefinitionService,
            GasService gasService,
            ConfirmationRouter confirmationRouter,
            ENSInteract ensInteract) {
        return new TransferTicketDetailViewModelFactory(
                genericWalletInteract, marketQueueService, createTransactionInteract, transferTicketDetailRouter, fetchTransactionsInteract,
                assetDisplayRouter, assetDefinitionService, gasService, confirmationRouter, ensInteract);
    }

    @Provides
    GenericWalletInteract provideFindDefaultWalletInteract(WalletRepositoryType walletRepository) {
        return new GenericWalletInteract(walletRepository);
    }

    @Provides
    TransferTicketDetailRouter provideTransferDetailRouter() {
        return new TransferTicketDetailRouter();
    }

    @Provides
    CreateTransactionInteract provideCreateTransactionInteract(TransactionRepositoryType transactionRepository, PasswordStore passwordStore) {
        return new CreateTransactionInteract(transactionRepository, passwordStore);
    }

    @Provides
    AssetDisplayRouter provideAssetDisplayRouter() {
        return new AssetDisplayRouter();
    }

    @Provides
    ConfirmationRouter provideConfirmationRouter() {
        return new ConfirmationRouter();
    }

    @Provides
    FetchTransactionsInteract provideFetchTransactionsInteract(TransactionRepositoryType transactionRepository,
                                                               TokenRepositoryType tokenRepositoryType) {
        return new FetchTransactionsInteract(transactionRepository, tokenRepositoryType);
    }

    @Provides
    ENSInteract provideENSInteract(TokenRepositoryType tokenRepository) {
        return new ENSInteract(tokenRepository);
    }
}
