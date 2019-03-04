package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import javax.inject.Inject;

import io.stormbird.wallet.interact.CreateWalletInteract;
import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.interact.FetchWalletsInteract;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.interact.SetDefaultWalletInteract;
import io.stormbird.wallet.router.HomeRouter;
import io.stormbird.wallet.router.ImportWalletRouter;

public class WalletsViewModelFactory implements ViewModelProvider.Factory {
    private final CreateWalletInteract createWalletInteract;
    private final SetDefaultWalletInteract setDefaultWalletInteract;
    private final FetchWalletsInteract fetchWalletsInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final ImportWalletRouter importWalletRouter;
    private final HomeRouter homeRouter;

    @Inject
    public WalletsViewModelFactory(
            CreateWalletInteract createWalletInteract,
            SetDefaultWalletInteract setDefaultWalletInteract,
            FetchWalletsInteract fetchWalletsInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            ImportWalletRouter importWalletRouter,
            HomeRouter homeRouter,
            FetchTokensInteract fetchTokensInteract,
            FindDefaultNetworkInteract findDefaultNetworkInteract) {
        this.createWalletInteract = createWalletInteract;
        this.setDefaultWalletInteract = setDefaultWalletInteract;
        this.fetchWalletsInteract = fetchWalletsInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.importWalletRouter = importWalletRouter;
        this.homeRouter = homeRouter;
        this.fetchTokensInteract = fetchTokensInteract;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new WalletsViewModel(
                createWalletInteract,
                setDefaultWalletInteract,
                fetchWalletsInteract,
                findDefaultWalletInteract,
                importWalletRouter,
                homeRouter,
                fetchTokensInteract,
                findDefaultNetworkInteract);
    }
}
