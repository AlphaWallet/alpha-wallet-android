package io.stormbird.wallet.di;

import io.stormbird.wallet.interact.ChangeTokenEnableInteract;
import io.stormbird.wallet.interact.FetchAllTokenInfoInteract;
import io.stormbird.wallet.repository.TokenRepositoryType;
import io.stormbird.wallet.viewmodel.TokenChangeCollectionViewModelFactory;

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
