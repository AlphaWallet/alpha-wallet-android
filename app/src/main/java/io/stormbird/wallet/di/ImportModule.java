package io.stormbird.wallet.di;

import io.stormbird.wallet.interact.ImportWalletInteract;
import io.stormbird.wallet.repository.PasswordStore;
import io.stormbird.wallet.repository.WalletRepositoryType;
import io.stormbird.wallet.viewmodel.ImportWalletViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class ImportModule {
    @Provides
    ImportWalletViewModelFactory provideImportWalletViewModelFactory(
            ImportWalletInteract importWalletInteract) {
        return new ImportWalletViewModelFactory(importWalletInteract);
    }

    @Provides
    ImportWalletInteract provideImportWalletInteract(
            WalletRepositoryType walletRepository, PasswordStore passwordStore) {
        return new ImportWalletInteract(walletRepository, passwordStore);
    }
}
