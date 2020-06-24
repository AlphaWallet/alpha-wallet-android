package com.alphawallet.app.di;

import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.repository.WalletRepositoryType;
import com.alphawallet.app.router.MyAddressRouter;
import com.alphawallet.app.viewmodel.QRScanningViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class QRScanningModule {
    @Provides
    QRScanningViewModelFactory provideQRScanningViewModelFactory(EthereumNetworkRepositoryType ethereumNetworkRepository,
                                                                 FindDefaultNetworkInteract findDefaultNetworkInteract,
                                                                 GenericWalletInteract genericWalletInteract,
                                                                 MyAddressRouter myAddressRouter)
    {
        return new QRScanningViewModelFactory(
                ethereumNetworkRepository,
                findDefaultNetworkInteract,
                genericWalletInteract,
                myAddressRouter);
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
    MyAddressRouter provideMyAddressRouter() {
        return new MyAddressRouter();
    }
}
