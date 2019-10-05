package com.alphawallet.app.repository;

import io.reactivex.disposables.Disposable;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Ticker;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.TokenTicker;
import com.alphawallet.app.entity.Wallet;

import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface TokenLocalSource {
    Single<Token> saveToken(Wallet wallet, Token token);
    Completable saveTokens(NetworkInfo networkInfo, Wallet wallet, Token[] items);
    void updateTokenBalance(NetworkInfo network, Wallet wallet, Token token);
    Token getTokenBalance(NetworkInfo network, Wallet wallet, String address);
    Map<Integer, Token> getTokenBalances(Wallet wallet, String address);
    void setEnable(NetworkInfo network, Wallet wallet, Token token, boolean isEnabled);

    Single<Token[]> fetchAllTokens(NetworkInfo networkInfo, Wallet wallet);
    Single<Token[]> fetchERC721Tokens(Wallet wallet);
    Single<Token> fetchEnabledToken(NetworkInfo networkInfo, Wallet wallet, String address);
    Single<Token[]> fetchEnabledTokensWithBalance(Wallet wallet);
    Single<Token> saveTicker(NetworkInfo network, Wallet wallet, Token token);
    Single<TokenTicker> fetchTicker(NetworkInfo network, Wallet wallet, Token tokens);

    Disposable setTokenTerminated(Token token, NetworkInfo network, Wallet wallet);

    Single<Token[]> saveERC721Tokens(Wallet wallet, Token[] tokens);

    Disposable storeBlockRead(Token token, Wallet wallet);
    Single<Token[]> saveERC20Tokens(Wallet wallet, Token[] tokens);
    void deleteRealmToken(int chainId, Wallet wallet, String address);
}
