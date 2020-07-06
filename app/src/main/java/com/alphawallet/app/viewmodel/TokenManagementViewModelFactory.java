package com.alphawallet.app.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;

import com.alphawallet.app.interact.ChangeTokenEnableInteract;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.router.AddTokenRouter;

import io.reactivex.annotations.NonNull;

public class TokenManagementViewModelFactory implements ViewModelProvider.Factory {

    private final TokenRepositoryType tokenRepository;
    private final ChangeTokenEnableInteract changeTokenEnableInteract;
    private final AddTokenRouter addTokenRouter;

    public TokenManagementViewModelFactory(TokenRepositoryType tokenRepository,
                                           ChangeTokenEnableInteract changeTokenEnableInteract,
                                           AddTokenRouter addTokenRouter)
    {
        this.tokenRepository = tokenRepository;
        this.changeTokenEnableInteract = changeTokenEnableInteract;
        this.addTokenRouter = addTokenRouter;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass)
    {
        return (T) new TokenManagementViewModel(
                tokenRepository,
                changeTokenEnableInteract,
                addTokenRouter
        );
    }
}
