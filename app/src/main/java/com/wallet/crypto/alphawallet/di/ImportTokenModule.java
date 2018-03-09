package com.wallet.crypto.alphawallet.di;

import com.wallet.crypto.alphawallet.interact.CreateTransactionInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import com.wallet.crypto.alphawallet.repository.PasswordStore;
import com.wallet.crypto.alphawallet.repository.TransactionRepositoryType;
import com.wallet.crypto.alphawallet.repository.WalletRepositoryType;
import com.wallet.crypto.alphawallet.viewmodel.ImportTokenViewModelFactory;

import dagger.Module;
import dagger.Provides;

/**
 * Created by James on 9/03/2018.
 */

@Module
public class ImportTokenModule {

    @Provides
    ImportTokenViewModelFactory importTokenViewModelFactory(
            FindDefaultWalletInteract findDefaultWalletInteract,
            CreateTransactionInteract createTransactionInteract) {
        return new ImportTokenViewModelFactory(
                findDefaultWalletInteract, createTransactionInteract);
    }

    @Provides
    FindDefaultWalletInteract provideFindDefaultWalletInteract(WalletRepositoryType walletRepository) {
        return new FindDefaultWalletInteract(walletRepository);
    }

    @Provides
    CreateTransactionInteract provideCreateTransactionInteract(TransactionRepositoryType transactionRepository, PasswordStore passwordStore) {
        return new CreateTransactionInteract(transactionRepository, passwordStore);
    }
}
