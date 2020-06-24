package com.alphawallet.app.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.EthereumNetworkRepositoryType;
import com.alphawallet.app.router.MyAddressRouter;

public class QRScanningViewModelFactory implements ViewModelProvider.Factory {

    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final GenericWalletInteract genericWalletInteract;
    private final MyAddressRouter myAddressRouter;

    public QRScanningViewModelFactory(EthereumNetworkRepositoryType ethereumNetworkRepository,
                                      FindDefaultNetworkInteract findDefaultNetworkInteract,
                                      GenericWalletInteract genericWalletInteract,
                                      MyAddressRouter myAddressRouter) {
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.genericWalletInteract = genericWalletInteract;
        this.myAddressRouter = myAddressRouter;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new QRScanningViewModel(
                ethereumNetworkRepository,
                findDefaultNetworkInteract,
                genericWalletInteract,
                myAddressRouter);
    }
}
