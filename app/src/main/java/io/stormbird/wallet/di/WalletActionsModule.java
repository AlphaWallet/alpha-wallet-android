package io.stormbird.wallet.di;

import dagger.Module;
import dagger.Provides;
import io.stormbird.wallet.interact.DeleteWalletInteract;
import io.stormbird.wallet.interact.ExportWalletInteract;
import io.stormbird.wallet.interact.FetchWalletsInteract;
import io.stormbird.wallet.repository.PasswordStore;
import io.stormbird.wallet.repository.WalletRepositoryType;
import io.stormbird.wallet.router.HomeRouter;
import io.stormbird.wallet.viewmodel.WalletActionsViewModelFactory;

@Module
class WalletActionsModule {
	@Provides
	WalletActionsViewModelFactory provideWalletActionsViewModelFactory(
			HomeRouter homeRouter,
			DeleteWalletInteract deleteWalletInteract,
			ExportWalletInteract exportWalletInteract,
			FetchWalletsInteract fetchWalletsInteract) {
		return new WalletActionsViewModelFactory(
				homeRouter,
				deleteWalletInteract,
				exportWalletInteract,
				fetchWalletsInteract);
	}

	@Provides
	HomeRouter provideHomeRouter() {
		return new HomeRouter();
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
