package io.stormbird.wallet.di;

import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.repository.WalletRepositoryType;
import io.stormbird.wallet.router.ManageWalletsRouter;

import dagger.Module;
import dagger.Provides;

@Module
class SettingsFragmentModule {
    @Provides
    FindDefaultWalletInteract provideFindDefaultWalletInteract(WalletRepositoryType walletRepository) {
        return new FindDefaultWalletInteract(walletRepository);
    }

    @Provides
    ManageWalletsRouter provideManageWalletsRouter() {
        return new ManageWalletsRouter();
    }
}
