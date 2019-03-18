package io.stormbird.wallet.di;

import dagger.Module;
import dagger.Provides;
import io.stormbird.wallet.interact.CreateTransactionInteract;
import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.repository.PasswordStore;
import io.stormbird.wallet.repository.TokenRepositoryType;
import io.stormbird.wallet.repository.TransactionRepositoryType;
import io.stormbird.wallet.repository.WalletRepositoryType;
import io.stormbird.wallet.router.ConfirmationRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.viewmodel.DappBrowserViewModelFactory;

@Module
public class DappBrowserModule {
    @Provides
    DappBrowserViewModelFactory provideWalletViewModelFactory(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            AssetDefinitionService assetDefinitionService,
            CreateTransactionInteract createTransactionInteract,
            FetchTokensInteract fetchTokensInteract,
            ConfirmationRouter confirmationRouter,
            EthereumNetworkRepositoryType ethereumNetworkRepository) {
        return new DappBrowserViewModelFactory(
                findDefaultNetworkInteract,
                findDefaultWalletInteract,
                assetDefinitionService,
                createTransactionInteract,
                fetchTokensInteract,
                confirmationRouter,
                ethereumNetworkRepository);
    }

    @Provides
    FindDefaultNetworkInteract provideFindDefaultNetworkInteract(
            EthereumNetworkRepositoryType ethereumNetworkRepositoryType) {
        return new FindDefaultNetworkInteract(ethereumNetworkRepositoryType);
    }

    @Provides
    ConfirmationRouter provideConfirmationRouter() {
        return new ConfirmationRouter();
    }

    @Provides
    FindDefaultWalletInteract provideFindDefaultWalletInteract(WalletRepositoryType walletRepository)
    {
        return new FindDefaultWalletInteract(walletRepository);
    }

    @Provides
    CreateTransactionInteract provideCreateTransactionInteract(TransactionRepositoryType transactionRepository, PasswordStore passwordStore) {
        return new CreateTransactionInteract(transactionRepository, passwordStore);
    }

    @Provides
    FetchTokensInteract provideFetchTokensInteract(TokenRepositoryType tokenRepository) {
        return new FetchTokensInteract(tokenRepository);
    }
}
