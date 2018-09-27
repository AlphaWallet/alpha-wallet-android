package io.stormbird.wallet.viewmodel;


import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import io.stormbird.wallet.interact.AddTokenInteract;
import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.interact.FindDefaultWalletInteract;
import io.stormbird.wallet.interact.GetDefaultWalletBalance;
import io.stormbird.wallet.interact.SetupTokensInteract;
import io.stormbird.wallet.router.AddTokenRouter;
import io.stormbird.wallet.router.AssetDisplayRouter;
import io.stormbird.wallet.router.ChangeTokenCollectionRouter;
import io.stormbird.wallet.router.SendTokenRouter;
import io.stormbird.wallet.service.AssetDefinitionService;
import io.stormbird.wallet.service.TokensService;

public class WalletViewModelFactory implements ViewModelProvider.Factory {
    private final FetchTokensInteract fetchTokensInteract;
    private final AddTokenRouter addTokenRouter;
    private final SendTokenRouter sendTokenRouter;
    private final AssetDisplayRouter assetDisplayRouter;
    private final ChangeTokenCollectionRouter changeTokenCollectionRouter;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final GetDefaultWalletBalance getDefaultWalletBalance;
    private final AddTokenInteract addTokenInteract;
    private final SetupTokensInteract setupTokensInteract;
    private final AssetDefinitionService assetDefinitionService;
    private final TokensService tokensService;

    public WalletViewModelFactory(/*FindDefaultNetworkInteract findDefaultNetworkInteract,*/
                                  FetchTokensInteract fetchTokensInteract,
                                  AddTokenRouter addTokenRouter,
                                  SendTokenRouter sendTokenRouter,
                                  ChangeTokenCollectionRouter changeTokenCollectionRouter,
                                  AssetDisplayRouter assetDisplayRouter,
                                  FindDefaultNetworkInteract findDefaultNetworkInteract,
                                  FindDefaultWalletInteract findDefaultWalletInteract,
                                  GetDefaultWalletBalance getDefaultWalletBalance,
                                  AddTokenInteract addTokenInteract,
                                  SetupTokensInteract setupTokensInteract,
                                  AssetDefinitionService assetDefinitionService,
                                  TokensService tokensService) {
        //this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.fetchTokensInteract = fetchTokensInteract;
        this.addTokenRouter = addTokenRouter;
        this.sendTokenRouter = sendTokenRouter;
        this.assetDisplayRouter = assetDisplayRouter;
        this.changeTokenCollectionRouter = changeTokenCollectionRouter;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.getDefaultWalletBalance = getDefaultWalletBalance;
        this.addTokenInteract = addTokenInteract;
        this.setupTokensInteract = setupTokensInteract;
        this.assetDefinitionService = assetDefinitionService;
        this.tokensService = tokensService;

    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new WalletViewModel(
                /*findDefaultNetworkInteract,*/
                fetchTokensInteract,
                addTokenRouter,
                sendTokenRouter,
                changeTokenCollectionRouter,
                assetDisplayRouter,
                findDefaultNetworkInteract,
                findDefaultWalletInteract,
                getDefaultWalletBalance,
                addTokenInteract,
                setupTokensInteract,
                assetDefinitionService,
                tokensService);
    }
}
