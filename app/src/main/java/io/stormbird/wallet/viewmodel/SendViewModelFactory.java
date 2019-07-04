package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import io.reactivex.annotations.NonNull;
import io.stormbird.wallet.interact.ENSInteract;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.router.ConfirmationRouter;
import io.stormbird.wallet.router.MyAddressRouter;
import io.stormbird.wallet.service.GasService;
import io.stormbird.wallet.service.TokensService;

public class SendViewModelFactory implements ViewModelProvider.Factory {

    private final ConfirmationRouter confirmationRouter;
    private final MyAddressRouter myAddressRouter;
    private final ENSInteract ensInteract;
    private final EthereumNetworkRepositoryType networkRepository;
    private final TokensService tokensService;
    private final GasService gasService;

    public SendViewModelFactory(ConfirmationRouter confirmationRouter,
                                MyAddressRouter myAddressRouter,
                                ENSInteract ensInteract,
                                EthereumNetworkRepositoryType networkRepository,
                                TokensService tokensService,
                                GasService gasService) {
        this.confirmationRouter = confirmationRouter;
        this.myAddressRouter = myAddressRouter;
        this.ensInteract = ensInteract;
        this.networkRepository = networkRepository;
        this.tokensService = tokensService;
        this.gasService = gasService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new SendViewModel(confirmationRouter, myAddressRouter, ensInteract, networkRepository, tokensService, gasService);
    }
}
