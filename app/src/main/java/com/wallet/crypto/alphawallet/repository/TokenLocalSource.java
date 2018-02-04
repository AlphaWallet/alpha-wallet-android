package com.wallet.crypto.trustapp.repository;

import com.wallet.crypto.trustapp.entity.NetworkInfo;
import com.wallet.crypto.trustapp.entity.Token;
import com.wallet.crypto.trustapp.entity.TokenTicker;
import com.wallet.crypto.trustapp.entity.Wallet;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface TokenLocalSource {
    Completable saveTokens(NetworkInfo networkInfo, Wallet wallet, Token[] items);
    void updateTokenBalance(NetworkInfo network, Wallet wallet, Token token);
    void setEnable(NetworkInfo network, Wallet wallet, Token token, boolean isEnabled);

    Single<Token[]> fetchEnabledTokens(NetworkInfo networkInfo, Wallet wallet);
    Single<Token[]> fetchAllTokens(NetworkInfo networkInfo, Wallet wallet);

    Completable saveTickers(NetworkInfo network, Wallet wallet, TokenTicker[] tokenTickers);
    Single<TokenTicker[]> fetchTickers(NetworkInfo network, Wallet wallet, Token[] tokens);
}
