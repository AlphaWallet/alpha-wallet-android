package io.stormbird.wallet.di;

import dagger.Module;
import dagger.Provides;
import io.stormbird.wallet.interact.DeleteWalletInteract;
import io.stormbird.wallet.interact.ExportWalletInteract;
import io.stormbird.wallet.interact.FetchWalletsInteract;
import io.stormbird.wallet.repository.WalletRepositoryType;
import io.stormbird.wallet.service.KeyService;
import io.stormbird.wallet.viewmodel.BackupKeyViewModelFactory;

@Module
public class BackupKeyModule {
    @Provides
    BackupKeyViewModelFactory provideBackupKeyViewModelFactory(
            KeyService keyService,
            ExportWalletInteract exportWalletInteract,
            FetchWalletsInteract fetchWalletsInteract) {
        return new BackupKeyViewModelFactory(
                keyService,
                exportWalletInteract,
                fetchWalletsInteract);
    }

    @Provides
    ExportWalletInteract provideExportWalletInteract(
            WalletRepositoryType walletRepository) {
        return new ExportWalletInteract(walletRepository);
    }

    @Provides
    FetchWalletsInteract provideFetchAccountsInteract(WalletRepositoryType accountRepository) {
        return new FetchWalletsInteract(accountRepository);
    }
}