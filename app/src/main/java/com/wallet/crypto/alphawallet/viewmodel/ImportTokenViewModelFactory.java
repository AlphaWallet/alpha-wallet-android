package com.wallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;
;import com.wallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import com.wallet.crypto.alphawallet.service.ImportTokenService;

/**
 * Created by James on 9/03/2018.
 */

public class ImportTokenViewModelFactory implements ViewModelProvider.Factory {

    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final ImportTokenService importTokenService;

    public ImportTokenViewModelFactory(FindDefaultWalletInteract findDefaultWalletInteract,
                                       ImportTokenService importTokenService) {
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.importTokenService = importTokenService;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new ImportTokenViewModel(findDefaultWalletInteract, importTokenService);
    }
}

