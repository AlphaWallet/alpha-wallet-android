package io.stormbird.wallet.viewmodel;


import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.interact.GetDefaultWalletBalance;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.repository.LocaleRepositoryType;
import io.stormbird.wallet.repository.PreferenceRepositoryType;
import io.stormbird.wallet.router.HelpRouter;
import io.stormbird.wallet.router.HomeRouter;
import io.stormbird.wallet.router.ManageWalletsRouter;
import io.stormbird.wallet.router.MyAddressRouter;

public class NewSettingsViewModelFactory implements ViewModelProvider.Factory {
    private final MyAddressRouter myAddressRouter;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final GetDefaultWalletBalance getDefaultWalletBalance;
    private final HelpRouter helpRouter;
    private final EthereumNetworkRepositoryType ethereumNetworkRepository;
    private final ManageWalletsRouter manageWalletsRouter;
    private final HomeRouter homeRouter;
    private final PreferenceRepositoryType preferenceRepository;
    private final LocaleRepositoryType localeRepository;

    public NewSettingsViewModelFactory(
            FindDefaultNetworkInteract findDefaultNetworkInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            GetDefaultWalletBalance getDefaultWalletBalance,
            MyAddressRouter myAddressRouter,
            HelpRouter helpRouter,
            EthereumNetworkRepositoryType ethereumNetworkRepository,
            ManageWalletsRouter manageWalletsRouter,
            HomeRouter homeRouter,
            PreferenceRepositoryType preferenceRepository,
            LocaleRepositoryType localeRepository) {
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.getDefaultWalletBalance = getDefaultWalletBalance;
        this.myAddressRouter = myAddressRouter;
        this.helpRouter = helpRouter;
        this.ethereumNetworkRepository = ethereumNetworkRepository;
        this.manageWalletsRouter = manageWalletsRouter;
        this.homeRouter = homeRouter;
        this.preferenceRepository = preferenceRepository;
        this.localeRepository = localeRepository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new NewSettingsViewModel(
                findDefaultNetworkInteract,
                findDefaultWalletInteract,
                getDefaultWalletBalance,
                myAddressRouter,
                helpRouter,
                ethereumNetworkRepository,
                manageWalletsRouter,
                homeRouter,
                preferenceRepository,
                localeRepository
        );
    }
}
