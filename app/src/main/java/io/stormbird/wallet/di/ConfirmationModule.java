package io.stormbird.wallet.di;


import io.stormbird.wallet.interact.CreateTransactionInteract;
import io.stormbird.wallet.interact.FetchGasSettingsInteract;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.repository.*;
import io.stormbird.wallet.router.GasSettingsRouter;
import io.stormbird.wallet.service.MarketQueueService;
import io.stormbird.wallet.service.TokensService;
import io.stormbird.wallet.viewmodel.ConfirmationViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
public class ConfirmationModule {
    @Provides
    public ConfirmationViewModelFactory provideConfirmationViewModelFactory(
            FindDefaultWalletInteract findDefaultWalletInteract,
            FetchGasSettingsInteract fetchGasSettingsInteract,
            CreateTransactionInteract createTransactionInteract,
            GasSettingsRouter gasSettingsRouter,
            MarketQueueService marketQueueService,
            TokensService tokensService,
            FindDefaultNetworkInteract findDefaultNetworkInteract
    ) {
        return new ConfirmationViewModelFactory(findDefaultWalletInteract, fetchGasSettingsInteract, createTransactionInteract, gasSettingsRouter, marketQueueService, tokensService, findDefaultNetworkInteract);
    }

    @Provides
    FindDefaultWalletInteract provideFindDefaultWalletInteract(WalletRepositoryType walletRepository) {
        return new FindDefaultWalletInteract(walletRepository);
    }

    @Provides
    FetchGasSettingsInteract provideFetchGasSettingsInteract(GasSettingsRepositoryType gasSettingsRepository) {
        return new FetchGasSettingsInteract(gasSettingsRepository);
    }

    @Provides
    CreateTransactionInteract provideCreateTransactionInteract(TransactionRepositoryType transactionRepository, PasswordStore passwordStore) {
        return new CreateTransactionInteract(transactionRepository, passwordStore);
    }

    @Provides
    GasSettingsRouter provideGasSettingsRouter() {
        return new GasSettingsRouter();
    }

    @Provides
    FindDefaultNetworkInteract provideFindDefaultNetworkInteract(
            EthereumNetworkRepositoryType networkRepository) {
        return new FindDefaultNetworkInteract(networkRepository);
    }
}
