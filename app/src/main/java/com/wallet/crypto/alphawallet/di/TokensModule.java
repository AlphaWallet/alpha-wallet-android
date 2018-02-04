package com.wallet.crypto.trustapp.di;

import com.wallet.crypto.trustapp.interact.FetchTokensInteract;
import com.wallet.crypto.trustapp.interact.FindDefaultNetworkInteract;
import com.wallet.crypto.trustapp.repository.EthereumNetworkRepositoryType;
import com.wallet.crypto.trustapp.repository.TokenRepositoryType;
import com.wallet.crypto.trustapp.router.AddTokenRouter;
import com.wallet.crypto.trustapp.router.ChangeTokenCollectionRouter;
import com.wallet.crypto.trustapp.router.SendTokenRouter;
import com.wallet.crypto.trustapp.router.TransactionsRouter;
import com.wallet.crypto.trustapp.router.UseTokenRouter;
import com.wallet.crypto.trustapp.viewmodel.TokensViewModelFactory;

import dagger.Module;
import dagger.Provides;

@Module
class TokensModule {

    @Provides
    TokensViewModelFactory provideTokensViewModelFactory(
            FetchTokensInteract fetchTokensInteract,
            AddTokenRouter addTokenRouter,
            SendTokenRouter sendTokenRouter,
            TransactionsRouter transactionsRouter,
            ChangeTokenCollectionRouter changeTokenCollectionRouter,
            UseTokenRouter useTokenRouter) {
        return new TokensViewModelFactory(
                fetchTokensInteract,
                addTokenRouter,
                sendTokenRouter,
                transactionsRouter,
                changeTokenCollectionRouter,
                useTokenRouter);
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
    UseTokenRouter provideUseTokenRouter() {
        return new UseTokenRouter();
    }

    @Provides
    TransactionsRouter provideTransactionsRouter() {
        return new TransactionsRouter();
    }

    @Provides
    ChangeTokenCollectionRouter provideChangeTokenCollectionRouter() {
        return new ChangeTokenCollectionRouter();
    }
}
