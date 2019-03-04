package io.stormbird.wallet.di;

import dagger.Module;
import dagger.Provides;
import io.stormbird.wallet.interact.CreateWalletInteract;
import io.stormbird.wallet.interact.DeleteWalletInteract;
import io.stormbird.wallet.interact.ExportWalletInteract;
import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.interact.FetchWalletsInteract;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.interact.SetDefaultWalletInteract;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.repository.PasswordStore;
import io.stormbird.wallet.repository.TokenRepositoryType;
import io.stormbird.wallet.repository.WalletRepositoryType;
import io.stormbird.wallet.router.HomeRouter;
import io.stormbird.wallet.router.ImportWalletRouter;
import io.stormbird.wallet.viewmodel.WalletActionsViewModelFactory;
import io.stormbird.wallet.viewmodel.WalletsViewModelFactory;

@Module
class WalletActionsModule {
	@Provides
	WalletActionsViewModelFactory provideWalletActionsViewModelFactory(
			FetchWalletsInteract fetchWalletsInteract) {
		return new WalletActionsViewModelFactory(fetchWalletsInteract);
	}

	@Provides
    FetchWalletsInteract provideFetchAccountsInteract(WalletRepositoryType accountRepository) {
		return new FetchWalletsInteract(accountRepository);
	}
}
