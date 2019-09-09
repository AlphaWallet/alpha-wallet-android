package io.stormbird.wallet.di;

import dagger.Module;
import dagger.Provides;
import io.stormbird.wallet.interact.*;
import io.stormbird.wallet.repository.*;
import io.stormbird.wallet.service.AlphaWalletService;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.GasService;
import io.stormbird.wallet.service.KeyService;
import io.stormbird.wallet.viewmodel.ImportTokenViewModelFactory;

/**
 * Created by James on 9/03/2018.
 */

@Module
public class ImportTokenModule {

    @Provides
    ImportTokenViewModelFactory importTokenViewModelFactory(
            GenericWalletInteract genericWalletInteract,
            CreateTransactionInteract createTransactionInteract,
            FetchTokensInteract fetchTokensInteract,
            SetupTokensInteract setupTokensInteract,
            AlphaWalletService alphaWalletService,
            AddTokenInteract addTokenInteract,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            AssetDefinitionService assetDefinitionService,
            FetchTransactionsInteract fetchTransactionsInteract,
            GasService gasService,
            KeyService keyService) {
        return new ImportTokenViewModelFactory(
                genericWalletInteract, createTransactionInteract, fetchTokensInteract, setupTokensInteract, alphaWalletService, addTokenInteract, ethereumNetworkRepository, assetDefinitionService, fetchTransactionsInteract, gasService, keyService);
    }

    @Provides
    GenericWalletInteract provideFindDefaultWalletInteract(WalletRepositoryType walletRepository) {
        return new GenericWalletInteract(walletRepository);
    }

    @Provides
    CreateTransactionInteract provideCreateTransactionInteract(TransactionRepositoryType transactionRepository) {
        return new CreateTransactionInteract(transactionRepository);
    }

    @Provides
    FetchTokensInteract provideFetchTokensInteract(TokenRepositoryType tokenRepository) {
        return new FetchTokensInteract(tokenRepository);
    }

    @Provides
    SetupTokensInteract provideSetupTokensInteract(TokenRepositoryType tokenRepository) {
        return new SetupTokensInteract(tokenRepository);
    }

    @Provides
    AddTokenInteract provideAddTokenInteract(
            TokenRepositoryType tokenRepository) {
        return new AddTokenInteract(tokenRepository);
    }

    @Provides
    FetchTransactionsInteract provideFetchTransactionsInteract(TransactionRepositoryType transactionRepository, TokenRepositoryType tokenRepositoryType) {
        return new FetchTransactionsInteract(transactionRepository, tokenRepositoryType);
    }
}
