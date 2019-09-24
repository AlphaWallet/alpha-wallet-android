package com.alphawallet.app.di;

import com.alphawallet.app.interact.ImportWalletInteract;
import com.alphawallet.app.repository.WalletRepositoryType;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.viewmodel.ImportWalletViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class ImportModule {
    @Provides
    ImportWalletViewModelFactory provideImportWalletViewModelFactory(
            ImportWalletInteract importWalletInteract, KeyService keyService, GasService gasService) {
        return new ImportWalletViewModelFactory(importWalletInteract, keyService, gasService);
    }

    @Provides
    ImportWalletInteract provideImportWalletInteract(
            WalletRepositoryType walletRepository) {
        return new ImportWalletInteract(walletRepository);
    }
}
