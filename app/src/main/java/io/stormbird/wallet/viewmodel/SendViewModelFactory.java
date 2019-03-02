package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;

import io.reactivex.annotations.NonNull;
import io.stormbird.wallet.interact.ENSInteract;
import io.stormbird.wallet.interact.FetchGasSettingsInteract;
import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.router.ConfirmationRouter;
import io.stormbird.wallet.router.MyAddressRouter;
import io.stormbird.wallet.service.AssetDefinitionService;

public class SendViewModelFactory implements ViewModelProvider.Factory {

    private final ConfirmationRouter confirmationRouter;
    private final MyAddressRouter myAddressRouter;
    private final ENSInteract ensInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final EthereumNetworkRepositoryType networkRepository;

    public SendViewModelFactory(ConfirmationRouter confirmationRouter,
                                MyAddressRouter myAddressRouter,
                                ENSInteract ensInteract,
                                AssetDefinitionService assetDefinitionService,
                                EthereumNetworkRepositoryType networkRepository) {
        this.confirmationRouter = confirmationRouter;
        this.myAddressRouter = myAddressRouter;
        this.ensInteract = ensInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.networkRepository = networkRepository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new SendViewModel(confirmationRouter, myAddressRouter, ensInteract, assetDefinitionService, networkRepository);
    }
}
