package io.awallet.crypto.alphawallet.repository;

import io.awallet.crypto.alphawallet.entity.NetworkInfo;
import io.awallet.crypto.alphawallet.entity.Token;
import io.awallet.crypto.alphawallet.entity.TokenTicker;
import io.awallet.crypto.alphawallet.entity.Wallet;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;

public interface TokenLocalSource {
    Single<Token[]> saveTokensList(NetworkInfo networkInfo, Wallet wallet, Token[] items);
    Completable saveTokens(NetworkInfo networkInfo, Wallet wallet, Token[] items);
    void updateTokenBalance(NetworkInfo network, Wallet wallet, Token token);
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
}
