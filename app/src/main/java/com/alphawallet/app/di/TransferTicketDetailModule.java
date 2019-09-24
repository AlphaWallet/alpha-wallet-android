package com.alphawallet.app.di;

import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.interact.ENSInteract;
import com.alphawallet.app.interact.FetchTransactionsInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.repository.TransactionRepositoryType;
import com.alphawallet.app.repository.WalletRepositoryType;
import com.alphawallet.app.router.AssetDisplayRouter;
import com.alphawallet.app.router.ConfirmationRouter;
import com.alphawallet.app.router.TransferTicketDetailRouter;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.viewmodel.TransferTicketDetailViewModelFactory;

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
            KeyService keyService,
            CreateTransactionInteract createTransactionInteract,
            TransferTicketDetailRouter transferTicketDetailRouter,
            FetchTransactionsInteract fetchTransactionsInteract,
            AssetDisplayRouter assetDisplayRouter,
            AssetDefinitionService assetDefinitionService,
            GasService gasService,
            ConfirmationRouter confirmationRouter,
            ENSInteract ensInteract) {
        return new TransferTicketDetailViewModelFactory(
                genericWalletInteract, keyService, createTransactionInteract, transferTicketDetailRouter, fetchTransactionsInteract,
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
    CreateTransactionInteract provideCreateTransactionInteract(TransactionRepositoryType transactionRepository) {
        return new CreateTransactionInteract(transactionRepository);
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
