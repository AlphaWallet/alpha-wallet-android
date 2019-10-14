package com.alphawallet.app.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import javax.inject.Inject;

import com.alphawallet.app.interact.FetchTokensInteract;
import com.alphawallet.app.interact.FetchWalletsInteract;
import com.alphawallet.app.interact.FindDefaultNetworkInteract;
import com.alphawallet.app.interact.GenericWalletInteract;
import com.alphawallet.app.interact.SetDefaultWalletInteract;
import com.alphawallet.app.router.HomeRouter;
import com.alphawallet.app.router.ImportWalletRouter;
import com.alphawallet.app.service.GasService;
import com.alphawallet.app.service.KeyService;
import com.alphawallet.app.service.TokensService;

public class WalletsViewModelFactory implements ViewModelProvider.Factory {
    private final SetDefaultWalletInteract setDefaultWalletInteract;
    private final FetchWalletsInteract fetchWalletsInteract;
    private final GenericWalletInteract genericWalletInteract;
    private final FetchTokensInteract fetchTokensInteract;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final ImportWalletRouter importWalletRouter;
    private final HomeRouter homeRouter;
    private final KeyService keyService;
    private final GasService gasService;
    private final TokensService tokensService;

    @Inject
    public WalletsViewModelFactory(
            SetDefaultWalletInteract setDefaultWalletInteract,
            FetchWalletsInteract fetchWalletsInteract,
            GenericWalletInteract genericWalletInteract,
            ImportWalletRouter importWalletRouter,
            HomeRouter homeRouter,
            FetchTokensInteract fetchTokensInteract,
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            KeyService keyService,
            GasService gasService,
            TokensService tokensService) {
        this.setDefaultWalletInteract = setDefaultWalletInteract;
        this.fetchWalletsInteract = fetchWalletsInteract;
        this.genericWalletInteract = genericWalletInteract;
        this.importWalletRouter = importWalletRouter;
        this.homeRouter = homeRouter;
        this.fetchTokensInteract = fetchTokensInteract;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.keyService = keyService;
        this.gasService = gasService;
        this.tokensService = tokensService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new WalletsViewModel(
                setDefaultWalletInteract,
                fetchWalletsInteract,
                genericWalletInteract,
                importWalletRouter,
                homeRouter,
                fetchTokensInteract,
                findDefaultNetworkInteract,
                keyService,
                gasService,
                tokensService);
    }
}
