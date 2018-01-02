package com.wallet.crypto.trustapp.repository;

import com.wallet.crypto.trustapp.entity.Token;
import com.wallet.crypto.trustapp.service.TokenExplorerClientType;

import io.reactivex.Single;

public class TokenRepository implements TokenRepositoryType {

    private final TokenExplorerClientType tokenNetworkService;

    public TokenRepository(TokenExplorerClientType tokenNetworkService) {
        this.tokenNetworkService = tokenNetworkService;
    }

    @Override
    public Single<Token[]> fetch(String walletAddress) {
        return Single.fromObservable(tokenNetworkService.fetch(walletAddress));
    }
}
