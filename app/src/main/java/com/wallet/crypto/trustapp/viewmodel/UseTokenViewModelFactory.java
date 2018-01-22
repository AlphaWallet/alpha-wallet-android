package com.wallet.crypto.trustapp.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import com.wallet.crypto.trustapp.interact.AddTokenInteract;
import com.wallet.crypto.trustapp.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.trustapp.interact.FindDefaultWalletInteract;
import com.wallet.crypto.trustapp.interact.SetupTokensInteract;
import com.wallet.crypto.trustapp.interact.UseTokenInteract;
import com.wallet.crypto.trustapp.router.MyTokensRouter;

/**
 * Created by James on 22/01/2018.
 */

public class UseTokenViewModelFactory implements ViewModelProvider.Factory {

    private final UseTokenInteract useTokenInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final MyTokensRouter myTokensRouter;
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;

    public UseTokenViewModelFactory(
            UseTokenInteract useTokenInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            MyTokensRouter myTokensRouter,
            FindDefaultNetworkInteract findDefaultNetworkInteract) {
        this.useTokenInteract = useTokenInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.myTokensRouter = myTokensRouter;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new UseTokenViewModel(useTokenInteract, findDefaultWalletInteract, myTokensRouter, findDefaultNetworkInteract);
    }
}
