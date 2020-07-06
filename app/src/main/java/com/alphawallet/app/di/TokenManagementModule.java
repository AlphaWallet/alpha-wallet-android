package com.alphawallet.app.di;

import com.alphawallet.app.interact.ChangeTokenEnableInteract;
import com.alphawallet.app.repository.TokenRepositoryType;
import com.alphawallet.app.router.AddTokenRouter;
import com.alphawallet.app.viewmodel.TokenManagementViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class TokenManagementModule {
    @Provides
    TokenManagementViewModelFactory provideTokenManagementViewModelFactory(
            TokenRepositoryType tokenRepository,
            ChangeTokenEnableInteract changeTokenEnableInteract,
            AddTokenRouter addTokenRouter)
    {
        return new TokenManagementViewModelFactory(tokenRepository, changeTokenEnableInteract, addTokenRouter);
    }

    @Provides
    ChangeTokenEnableInteract provideChangeTokenEnableInteract(TokenRepositoryType tokenRepository) {
        return new ChangeTokenEnableInteract(tokenRepository);
    }

    @Provides
    AddTokenRouter provideAddTokenRouter() {
        return new AddTokenRouter();
    }
}
