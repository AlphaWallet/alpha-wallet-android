package com.alphawallet.app.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;
import com.alphawallet.app.interact.CreateTransactionInteract;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.repository.PreferenceRepositoryType;
import com.alphawallet.app.router.GasSettingsRouter;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.TokensService;

public class ConfirmationViewModelFactory implements ViewModelProvider.Factory {

    private GenericWalletInteract genericWalletInteract;
    private GasService gasService;
    private CreateTransactionInteract createTransactionInteract;
    private GasSettingsRouter gasSettingsRouter;
    private TokensService tokensService;
    private FindDefaultNetworkInteract findDefaultNetworkInteract;
    private KeyService keyService;
    private final PreferenceRepositoryType preferenceRepositoryType;

    public ConfirmationViewModelFactory(GenericWalletInteract genericWalletInteract,
                                        GasService gasService,
                                        CreateTransactionInteract createTransactionInteract,
                                        GasSettingsRouter gasSettingsRouter,
                                        TokensService tokensService,
                                        FindDefaultNetworkInteract findDefaultNetworkInteract,
                                        KeyService keyService,
                                        PreferenceRepositoryType preferenceRepositoryType) {
        this.genericWalletInteract = genericWalletInteract;
        this.gasService = gasService;
        this.createTransactionInteract = createTransactionInteract;
        this.gasSettingsRouter = gasSettingsRouter;
        this.tokensService = tokensService;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.keyService = keyService;
        this.preferenceRepositoryType = preferenceRepositoryType;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new ConfirmationViewModel(
                genericWalletInteract,
                gasService,
                createTransactionInteract,
                gasSettingsRouter,
                tokensService,
                findDefaultNetworkInteract,
                keyService,
                preferenceRepositoryType);
    }
}
