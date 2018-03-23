package io.awallet.crypto.alphawallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;
;import io.awallet.crypto.alphawallet.interact.CreateTransactionInteract;
import io.awallet.crypto.alphawallet.interact.FetchTokensInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultNetworkInteract;
import io.awallet.crypto.alphawallet.interact.FindDefaultWalletInteract;
import io.awallet.crypto.alphawallet.service.ImportTokenService;

/**
 * Created by James on 9/03/2018.
 */

public class ImportTokenViewModelFactory implements ViewModelProvider.Factory {

    private final FindDefaultWalletInteract findDefaultWalletInteract;
    private final CreateTransactionInteract createTransactionInteract;
    private final FetchTokensInteract fetchTokensInteract;

    public ImportTokenViewModelFactory(FindDefaultWalletInteract findDefaultWalletInteract,
                                       CreateTransactionInteract createTransactionInteract,
                                       FetchTokensInteract fetchTokensInteract) {
        this.findDefaultWalletInteract = findDefaultWalletInteract;
        this.createTransactionInteract = createTransactionInteract;
        this.fetchTokensInteract = fetchTokensInteract;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new ImportTokenViewModel(findDefaultWalletInteract, createTransactionInteract, fetchTokensInteract);
    }
}

