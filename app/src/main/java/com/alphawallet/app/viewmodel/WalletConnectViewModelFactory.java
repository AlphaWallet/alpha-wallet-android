package com.alphawallet.app.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.content.Context;
import android.support.annotation.NonNull;

import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.RealmManager;
import com.alphawallet.app.service.WalletConnectService;

import javax.inject.Inject;

public class WalletConnectViewModelFactory implements ViewModelProvider.Factory {
    private final KeyService keyService;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final CreateTransactionInteract createTransactionInteract;
    private final GenericWalletInteract genericWalletInteract;
    private final RealmManager realmManager;
    private final Context context;

    @Inject
    public WalletConnectViewModelFactory(
            KeyService keyService,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            CreateTransactionInteract createTransactionInteract,
            GenericWalletInteract genericWalletInteract,
            RealmManager realmManager,
            Context context) {
        this.keyService = keyService;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.createTransactionInteract = createTransactionInteract;
        this.genericWalletInteract = genericWalletInteract;
        this.realmManager = realmManager;
        this.context = context;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new WalletConnectViewModel(
                keyService,
                findDefaultNetworkInteract,
                createTransactionInteract,
                genericWalletInteract,
                realmManager,
                context);
    }
}
