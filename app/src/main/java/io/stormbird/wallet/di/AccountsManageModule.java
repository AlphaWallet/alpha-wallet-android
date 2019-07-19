package io.stormbird.wallet.di;

import io.stormbird.wallet.interact.*;
import io.stormbird.wallet.interact.GenericWalletInteract;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.repository.PasswordStore;
import io.stormbird.wallet.repository.TokenRepositoryType;
import io.stormbird.wallet.repository.WalletRepositoryType;
import io.stormbird.wallet.router.HomeRouter;
import io.stormbird.wallet.router.ImportWalletRouter;
import io.stormbird.wallet.viewmodel.WalletsViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class AccountsManageModule {

	@Provides
    WalletsViewModelFactory provideAccountsManageViewModelFactory(
			CreateWalletInteract createWalletInteract,
			SetDefaultWalletInteract setDefaultWalletInteract,
			FetchWalletsInteract fetchWalletsInteract,
			GenericWalletInteract genericWalletInteract,
			ImportWalletRouter importWalletRouter,
			HomeRouter homeRouter,
			FetchTokensInteract fetchTokensInteract,
			FindDefaultNetworkInteract findDefaultNetworkInteract) {
		return new WalletsViewModelFactory(createWalletInteract,
                setDefaultWalletInteract,
                fetchWalletsInteract,
                                           genericWalletInteract,
                importWalletRouter,
                homeRouter,
				fetchTokensInteract,
				findDefaultNetworkInteract);
	}

	@Provides
    CreateWalletInteract provideCreateAccountInteract(
            WalletRepositoryType accountRepository, PasswordStore passwordStore) {
		return new CreateWalletInteract(accountRepository, passwordStore);
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
