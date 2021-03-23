package com.alphawallet.app.di;

import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.TransactionRepositoryType;
import com.alphawallet.app.repository.WalletRepositoryType;
import com.alphawallet.app.service.AssetDefinitionService;
import com.alphawallet.app.service.GasService2;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.TokensService;
import com.alphawallet.app.viewmodel.DappBrowserViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
public class DappBrowserModule {
    @Provides
    DappBrowserViewModelFactory provideWalletViewModelFactory(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            GenericWalletInteract genericWalletInteract,
            AssetDefinitionService assetDefinitionService,
            CreateTransactionInteract createTransactionInteract,
            TokensService tokensService,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            KeyService keyService,
            GasService2 gasService) {
        return new DappBrowserViewModelFactory(
                findDefaultNetworkInteract,
                genericWalletInteract,
                assetDefinitionService,
                createTransactionInteract,
                tokensService,
                ethereumNetworkRepository,
                keyService,
                gasService);
    }

    @Provides
    FindDefaultNetworkInteract provideFindDefaultNetworkInteract(
            EthereumNetworkRepositoryType ethereumNetworkRepositoryType) {
        return new FindDefaultNetworkInteract(ethereumNetworkRepositoryType);
    }

    @Provides
    GenericWalletInteract provideFindDefaultWalletInteract(WalletRepositoryType walletRepository)
    {
        return new GenericWalletInteract(walletRepository);
    }

    @Provides
    CreateTransactionInteract provideCreateTransactionInteract(TransactionRepositoryType transactionRepository) {
        return new CreateTransactionInteract(transactionRepository);
    }
}
