package io.stormbird.wallet.di;

import io.stormbird.wallet.interact.*;
import io.stormbird.wallet.interact.GenericWalletInteract;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.repository.TokenRepositoryType;
import io.stormbird.wallet.repository.WalletRepositoryType;
import io.stormbird.wallet.router.HomeRouter;
import io.stormbird.wallet.router.ImportWalletRouter;
import io.stormbird.wallet.service.GasService;
import io.stormbird.wallet.service.KeyService;
import io.stormbird.wallet.util.AWEnsResolver;
import io.stormbird.wallet.viewmodel.WalletsViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class AccountsManageModule {

	@Provides
    WalletsViewModelFactory provideAccountsManageViewModelFactory(
			SetDefaultWalletInteract setDefaultWalletInteract,
			FetchWalletsInteract fetchWalletsInteract,
			GenericWalletInteract genericWalletInteract,
			ImportWalletRouter importWalletRouter,
			HomeRouter homeRouter,
			FetchTokensInteract fetchTokensInteract,
			FindDefaultNetworkInteract findDefaultNetworkInteract,
			KeyService keyService,
			GasService gasService)
	{
		return new WalletsViewModelFactory(setDefaultWalletInteract,
										   fetchWalletsInteract,
										   genericWalletInteract,
										   importWalletRouter,
										   homeRouter,
										   fetchTokensInteract,
										   findDefaultNetworkInteract,
										   keyService,
										   gasService);
	}

	@Provides
    SetDefaultWalletInteract provideSetDefaultAccountInteract(WalletRepositoryType accountRepository) {
		return new SetDefaultWalletInteract(accountRepository);
	}

	@Provides
    FetchWalletsInteract provideFetchAccountsInteract(WalletRepositoryType accountRepository) {
		return new FetchWalletsInteract(accountRepository);
	}

	@Provides
    GenericWalletInteract provideFindDefaultAccountInteract(WalletRepositoryType accountRepository) {
		return new GenericWalletInteract(accountRepository);
	}

	@Provides
    ImportWalletRouter provideImportAccountRouter() {
		return new ImportWalletRouter();
	}

	@Provides
    HomeRouter provideHomeRouter() {
	    return new HomeRouter();
    }

	@Provides
	FetchTokensInteract provideFetchTokensInteract(TokenRepositoryType tokenRepository) {
		return new FetchTokensInteract(tokenRepository);
	}

	@Provides
	FindDefaultNetworkInteract provideFindDefaultNetworkInteract(
			EthereumNetworkRepositoryType networkRepository) {
		return new FindDefaultNetworkInteract(networkRepository);
	}
}
