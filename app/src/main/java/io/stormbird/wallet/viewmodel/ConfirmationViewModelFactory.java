package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import io.stormbird.wallet.interact.CreateTransactionInteract;
import io.stormbird.wallet.interact.FetchGasSettingsInteract;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.router.GasSettingsRouter;
import io.stormbird.wallet.service.MarketQueueService;
import io.stormbird.wallet.service.TokensService;

public class ConfirmationViewModelFactory implements ViewModelProvider.Factory {

    private FindDefaultWalletInteract findDefaultWalletInteract;
    private FetchGasSettingsInteract fetchGasSettingsInteract;
    private CreateTransactionInteract createTransactionInteract;
    private GasSettingsRouter gasSettingsRouter;
    private MarketQueueService marketQueueService;
    private TokensService tokensService;
    private FindDefaultNetworkInteract findDefaultNetworkInteract;

    public ConfirmationViewModelFactory(FindDefaultWalletInteract findDefaultWalletInteract,
                                        FetchGasSettingsInteract fetchGasSettingsInteract,
                                        CreateTransactionInteract createTransactionInteract,
                                        GasSettingsRouter gasSettingsRouter,
                                        MarketQueueService marketQueueService,
                                        TokensService tokensService,
                                        FindDefaultNetworkInteract findDefaultNetworkInteract) {
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.fetchGasSettingsInteract = fetchGasSettingsInteract;
        this.createTransactionInteract = createTransactionInteract;
        this.gasSettingsRouter = gasSettingsRouter;
        this.marketQueueService = marketQueueService;
        this.tokensService = tokensService;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new ConfirmationViewModel(findDefaultWalletInteract, fetchGasSettingsInteract, createTransactionInteract, gasSettingsRouter, marketQueueService, tokensService, findDefaultNetworkInteract);
    }
}
