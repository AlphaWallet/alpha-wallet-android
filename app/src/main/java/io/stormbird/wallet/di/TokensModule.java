package io.stormbird.wallet.di;

import io.stormbird.wallet.interact.FetchTokensInteract;
import io.stormbird.wallet.interact.FindDefaultNetworkInteract;
import io.stormbird.wallet.repository.EthereumNetworkRepositoryType;
import io.stormbird.wallet.repository.TokenRepositoryType;
import io.stormbird.wallet.router.AddTokenRouter;
import io.stormbird.wallet.router.AssetDisplayRouter;
import io.stormbird.wallet.router.ChangeTokenCollectionRouter;
import io.stormbird.wallet.router.HomeRouter;
import io.stormbird.wallet.router.SendTokenRouter;
import io.stormbird.wallet.viewmodel.TokensViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class TokensModule {

    @Provides
    TokensViewModelFactory provideTokensViewModelFactory(
            FetchTokensInteract fetchTokensInteract,
            AddTokenRouter addTokenRouter,
            SendTokenRouter sendTokenRouter,
            HomeRouter homeRouter,
            ChangeTokenCollectionRouter changeTokenCollectionRouter,
            AssetDisplayRouter assetDisplayRouter) {
        return new TokensViewModelFactory(
                fetchTokensInteract,
                addTokenRouter,
                sendTokenRouter,
                homeRouter,
                changeTokenCollectionRouter,
                assetDisplayRouter);
    }

    @Provides
    FindDefaultNetworkInteract provideFindDefaultNetworkInteract(
            EthereumNetworkRepositoryType networkRepository) {
        return new FindDefaultNetworkInteract(networkRepository);
    }

    @Provides
    FetchTokensInteract provideFetchTokensInteract(TokenRepositoryType tokenRepository) {
        return new FetchTokensInteract(tokenRepository);
    }

    @Provides
    AddTokenRouter provideAddTokenRouter() {
        return new AddTokenRouter();
    }

    @Provides
    SendTokenRouter provideSendTokenRouter() {
        return new SendTokenRouter();
    }

    @Provides
    AssetDisplayRouter provideRedeemTokenRouter() {
        return new AssetDisplayRouter();
    }

    @Provides
    HomeRouter provideHomeRouter() {
        return new HomeRouter();
    }

    @Provides
    ChangeTokenCollectionRouter provideChangeTokenCollectionRouter() {
        return new ChangeTokenCollectionRouter();
    }
}
