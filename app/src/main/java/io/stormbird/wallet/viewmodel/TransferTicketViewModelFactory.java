package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.router.TransferTicketDetailRouter;
import io.stormbird.wallet.service.AssetDefinitionService;

/**
 * Created by James on 16/02/2018.
 */

public class TransferTicketViewModelFactory implements ViewModelProvider.Factory {

    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final TransferTicketDetailRouter transferTicketDetailRouter;
    private final AssetDefinitionService assetDefinitionService;

    public TransferTicketViewModelFactory(
            FetchTokensInteract fetchTokensInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            TransferTicketDetailRouter transferTicketDetailRouter,
            AssetDefinitionService assetDefinitionService) {
        this.fetchTokensInteract = fetchTokensInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.transferTicketDetailRouter = transferTicketDetailRouter;
        this.assetDefinitionService = assetDefinitionService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new TransferTicketViewModel(fetchTokensInteract, findDefaultWalletInteract, findDefaultNetworkInteract, transferTicketDetailRouter, assetDefinitionService);
    }
}
