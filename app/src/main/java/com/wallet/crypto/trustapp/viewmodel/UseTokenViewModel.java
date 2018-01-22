package com.wallet.crypto.trustapp.viewmodel;

import android.arch.lifecycle.MutableLiveData;

import com.wallet.crypto.trustapp.interact.AddTokenInteract;
import com.wallet.crypto.trustapp.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.trustapp.interact.FindDefaultWalletInteract;
import com.wallet.crypto.trustapp.interact.SetupTokensInteract;
import com.wallet.crypto.trustapp.interact.UseTokenInteract;
import com.wallet.crypto.trustapp.router.MyTokensRouter;

/**
 * Created by James on 22/01/2018.
 */

public class UseTokenViewModel extends BaseViewModel {
    private final FindDefaultNetworkInteract findDefaultNetworkInteract;
    private final UseTokenInteract useTokenInteract;
    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final MyTokensRouter myTokensRouter;

    UseTokenViewModel(
            UseTokenInteract useTokenInteract,
            FindDefaultWalletInteract findDefaultWalletInteract,
            MyTokensRouter myTokensRouter,
            FindDefaultNetworkInteract findDefaultNetworkInteract) {
        this.useTokenInteract = useTokenInteract;
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.myTokensRouter = myTokensRouter;
        this.findDefaultNetworkInteract = findDefaultNetworkInteract;
    }
}
