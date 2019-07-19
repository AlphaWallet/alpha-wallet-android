package io.stormbird.wallet.viewmodel;


import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import io.stormbird.wallet.interact.GenericWalletInteract;
import io.stormbird.wallet.interact.GetDefaultWalletBalance;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.repository.LocaleRepositoryType;
import io.stormbird.wallet.repository.PreferenceRepositoryType;
import io.stormbird.wallet.router.HelpRouter;
import io.stormbird.wallet.router.HomeRouter;
import io.stormbird.wallet.router.ManageWalletsRouter;
import io.stormbird.wallet.router.MyAddressRouter;
import io.stormbird.wallet.service.TokensService;

public class NewSettingsViewModelFactory implements ViewModelProvider.Factory {
    private final MyAddressRouter myAddressRouter;
    private final GenericWalletInteract genericWalletInteract;
    private final GetDefaultWalletBalance getDefaultWalletBalance;
    private final HelpRouter helpRouter;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final ManageWalletsRouter manageWalletsRouter;
    private final HomeRouter homeRouter;
    private final PreferenceRepositoryType preferenceRepository;
    private final LocaleRepositoryType localeRepository;
    private final TokensService tokensService;

    public NewSettingsViewModelFactory(
            GenericWalletInteract genericWalletInteract,
            GetDefaultWalletBalance getDefaultWalletBalance,
            MyAddressRouter myAddressRouter,
            HelpRouter helpRouter,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            ManageWalletsRouter manageWalletsRouter,
            HomeRouter homeRouter,
            PreferenceRepositoryType preferenceRepository,
            LocaleRepositoryType localeRepository,
            TokensService tokensService) {
        this.genericWalletInteract = genericWalletInteract;
        this.getDefaultWalletBalance = getDefaultWalletBalance;
        this.myAddressRouter = myAddressRouter;
        this.helpRouter = helpRouter;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.manageWalletsRouter = manageWalletsRouter;
        this.homeRouter = homeRouter;
        this.preferenceRepository = preferenceRepository;
        this.localeRepository = localeRepository;
        this.tokensService = tokensService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new NewSettingsViewModel(
                genericWalletInteract,
                getDefaultWalletBalance,
                myAddressRouter,
                helpRouter,
                ethereumNetworkRepository,
                manageWalletsRouter,
                homeRouter,
                preferenceRepository,
                localeRepository,
                tokensService
        );
    }
}
