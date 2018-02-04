package com.wallet.crypto.alphawallet.di;

import com.wallet.crypto.alphawallet.interact.ChangeTokenEnableInteract;
import com.wallet.crypto.alphawallet.interact.FetchAllTokenInfoInteract;
import com.wallet.crypto.alphawallet.repository.TokenRepositoryType;
import com.wallet.crypto.alphawallet.viewmodel.TokenChangeCollectionViewModelFactory;

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
