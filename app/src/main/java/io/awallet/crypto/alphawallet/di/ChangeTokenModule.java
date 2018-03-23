package io.awallet.crypto.alphawallet.di;

import io.awallet.crypto.alphawallet.interact.ChangeTokenEnableInteract;
import io.awallet.crypto.alphawallet.interact.FetchAllTokenInfoInteract;
import io.awallet.crypto.alphawallet.repository.TokenRepositoryType;
import io.awallet.crypto.alphawallet.viewmodel.TokenChangeCollectionViewModelFactory;

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
