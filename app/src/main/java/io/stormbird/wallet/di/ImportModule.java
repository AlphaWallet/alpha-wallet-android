package io.stormbird.wallet.di;

import io.stormbird.wallet.interact.ImportWalletInteract;
import io.stormbird.wallet.repository.WalletRepositoryType;
import io.stormbird.wallet.service.GasService;
import io.stormbird.wallet.service.KeyService;
import io.stormbird.wallet.viewmodel.ImportWalletViewModelFactory;

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
