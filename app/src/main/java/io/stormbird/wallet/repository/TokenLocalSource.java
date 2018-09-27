package io.stormbird.wallet.repository;

import io.stormbird.wallet.entity.NetworkInfo;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.TokenTicker;
import io.stormbird.wallet.entity.Wallet;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;

public interface TokenLocalSource {
    Single<Token[]> saveTokensList(NetworkInfo networkInfo, Wallet wallet, Token[] items);
    Single<Token> saveToken(NetworkInfo networkInfo, Wallet wallet, Token token);
    Completable saveTokens(NetworkInfo networkInfo, Wallet wallet, Token[] items);
    void updateTokenBalance(NetworkInfo network, Wallet wallet, Token token);
    Token getTokenBalance(NetworkInfo network, Wallet wallet, String address);
    void updateTokenDestroyed(NetworkInfo network, Wallet wallet, Token token);
    void setEnable(NetworkInfo network, Wallet wallet, Token token, boolean isEnabled);
    void updateTokenBurn(NetworkInfo network, Wallet wallet, Token token, List<Integer> burn);

    Single<Token[]> fetchEnabledTokens(NetworkInfo networkInfo, Wallet wallet);
    Single<Token[]> fetchAllTokens(NetworkInfo networkInfo, Wallet wallet);
    Single<Token> fetchEnabledToken(NetworkInfo networkInfo, Wallet wallet, String address);
    Single<Token[]> fetchEnabledTokensWithBalance(NetworkInfo networkInfo, Wallet wallet);
    //Observable<Token> fetchEnabledTokensSequential(NetworkInfo networkInfo, Wallet wallet);
    Observable<List<Token>> fetchEnabledTokensSequentialList(NetworkInfo networkInfo, Wallet wallet);
    Completable saveTickers(NetworkInfo network, Wallet wallet, TokenTicker[] tokenTickers);
    Single<TokenTicker[]> fetchTickers(NetworkInfo network, Wallet wallet, Token[] tokens);

    void setTokenTerminated(NetworkInfo network, Wallet wallet, Token token);
}
