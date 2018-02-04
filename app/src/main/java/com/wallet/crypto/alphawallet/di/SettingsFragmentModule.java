package com.wallet.crypto.alphawallet.di;

import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import com.wallet.crypto.alphawallet.repository.WalletRepositoryType;
import com.wallet.crypto.alphawallet.router.ManageWalletsRouter;

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
