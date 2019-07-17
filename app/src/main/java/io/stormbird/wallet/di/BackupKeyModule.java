package io.stormbird.wallet.di;

import dagger.Module;
import dagger.Provides;
import io.stormbird.wallet.interact.DeleteWalletInteract;
import io.stormbird.wallet.interact.ExportWalletInteract;
import io.stormbird.wallet.interact.FetchWalletsInteract;
import io.stormbird.wallet.repository.PasswordStore;
import io.stormbird.wallet.repository.WalletRepositoryType;
import io.stormbird.wallet.viewmodel.BackupKeyViewModelFactory;

@Module
public class BackupKeyModule {
    @Provides
    BackupKeyViewModelFactory provideBackupKeyViewModelFactory(
            DeleteWalletInteract deleteWalletInteract,
            ExportWalletInteract exportWalletInteract,
            FetchWalletsInteract fetchWalletsInteract) {
        return new BackupKeyViewModelFactory(
                deleteWalletInteract,
                exportWalletInteract,
                fetchWalletsInteract);
    }

    @Provides
    DeleteWalletInteract provideDeleteAccountInteract(
            WalletRepositoryType accountRepository, PasswordStore store) {
        return new DeleteWalletInteract(accountRepository, store);
    }

    @Provides
    ExportWalletInteract provideExportWalletInteract(
            WalletRepositoryType walletRepository, PasswordStore passwordStore) {
        return new ExportWalletInteract(walletRepository, passwordStore);
    }

    @Provides
    FetchWalletsInteract provideFetchAccountsInteract(WalletRepositoryType accountRepository) {
        return new FetchWalletsInteract(accountRepository);
    }
}