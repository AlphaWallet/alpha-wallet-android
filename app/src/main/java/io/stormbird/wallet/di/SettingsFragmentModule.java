package io.stormbird.wallet.di;

import io.stormbird.wallet.interact.GenericWalletInteract;
import io.stormbird.wallet.repository.WalletRepositoryType;
import io.stormbird.wallet.router.ManageWalletsRouter;

import dagger.Module;
import dagger.Provides;

@Module
class SettingsFragmentModule {
    @Provides
    GenericWalletInteract provideFindDefaultWalletInteract(WalletRepositoryType walletRepository) {
        return new GenericWalletInteract(walletRepository);
    }

    @Provides
    ManageWalletsRouter provideManageWalletsRouter() {
        return new ManageWalletsRouter();
    }
}
