package com.wallet.crypto.trustapp.di;

import com.wallet.crypto.trustapp.interact.ChangeTokenEnableInteract;
import com.wallet.crypto.trustapp.interact.FetchAllTokenInfoInteract;
import com.wallet.crypto.trustapp.repository.TokenRepositoryType;
import com.wallet.crypto.trustapp.viewmodel.TokenChangeCollectionViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
public class ChangeTokenModule {

    @Provides
    TokenChangeCollectionViewModelFactory provideChangeTokenCollectionViewModelFactory(
            FetchAllTokenInfoInteract fetchAllTokenInfoInteract,
            ChangeTokenEnableInteract changeTokenEnableInteract) {
        return new TokenChangeCollectionViewModelFactory(
                fetchAllTokenInfoInteract,
                changeTokenEnableInteract);
    }

    @Provides
    FetchAllTokenInfoInteract provideFetchAllTokenInfoInteract(TokenRepositoryType tokenRepository) {
        return new FetchAllTokenInfoInteract(tokenRepository);
    }

    @Provides
    ChangeTokenEnableInteract provideChangeTokenEnableInteract(TokenRepositoryType tokenRepository) {
        return new ChangeTokenEnableInteract(tokenRepository);
    }
}
