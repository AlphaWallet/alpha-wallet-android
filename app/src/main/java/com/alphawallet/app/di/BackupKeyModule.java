package com.alphawallet.app.di;

import dagger.Module;
import dagger.Provides;

import com.alphawallet.app.interact.ExportWalletInteract;
import com.alphawallet.app.interact.FetchWalletsInteract;
import com.alphawallet.app.repository.WalletRepositoryType;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.viewmodel.BackupKeyViewModelFactory;

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