package io.stormbird.wallet.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import io.stormbird.wallet.interact.ChangeTokenEnableInteract;
import io.stormbird.wallet.interact.FetchAllTokenInfoInteract;

public class TokenChangeCollectionViewModelFactory implements ViewModelProvider.Factory {

    private final FetchAllTokenInfoInteract fetchAllTokenInfoInteract;
    private final ChangeTokenEnableInteract changeTokenEnableInteract;

    public TokenChangeCollectionViewModelFactory(
            FetchAllTokenInfoInteract fetchAllTokenInfoInteract,
            ChangeTokenEnableInteract changeTokenEnableInteract) {
        this.fetchAllTokenInfoInteract = fetchAllTokenInfoInteract;
        this.changeTokenEnableInteract = changeTokenEnableInteract;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new TokenChangeCollectionViewModel(
                fetchAllTokenInfoInteract,
                changeTokenEnableInteract);
    }
}
