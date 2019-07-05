package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;
import io.stormbird.wallet.interact.CreateTransactionInteract;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.router.GasSettingsRouter;
import io.stormbird.wallet.service.GasService;
import io.stormbird.wallet.service.TokensService;

public class ConfirmationViewModelFactory implements ViewModelProvider.Factory {

    private FindDefaultWalletInteract findDefaultWalletInteract;
    private GasService gasService;
    private CreateTransactionInteract createTransactionInteract;
    private GasSettingsRouter gasSettingsRouter;
    private TokensService tokensService;
    private FindDefaultNetworkInteract findDefaultNetworkInteract;

    public ConfirmationViewModelFactory(FindDefaultWalletInteract findDefaultWalletInteract,
                                        GasService gasService,
                                        CreateTransactionInteract createTransactionInteract,
                                        GasSettingsRouter gasSettingsRouter,
                                        TokensService tokensService,
                                        FindDefaultNetworkInteract findDefaultNetworkInteract) {
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.gasService = gasService;
        this.createTransactionInteract = createTransactionInteract;
        this.gasSettingsRouter = gasSettingsRouter;
        this.tokensService = tokensService;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new ConfirmationViewModel(findDefaultWalletInteract, gasService, createTransactionInteract, gasSettingsRouter, tokensService, findDefaultNetworkInteract);
    }
}
