package com.alphawallet.app.repository;

import io.reactivex.disposables.Disposable;

import com.alphawallet.app.entity.ContractType;
import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenTicker;
import com.alphawallet.app.entity.Wallet;

import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Single;

public interface TokenLocalSource {
    Single<Token> saveToken(Wallet wallet, Token token);
    Single<Token[]> saveTokens(Wallet wallet, Token[] items);
    void updateTokenBalance(NetworkInfo network, Wallet wallet, Token token);
    Token getTokenBalance(NetworkInfo network, Wallet wallet, String address);
    Map<Integer, Token> getTokenBalances(Wallet wallet, String address);
    void setEnable(NetworkInfo network, Wallet wallet, Token token, boolean isEnabled);

    Single<Token> fetchEnabledToken(NetworkInfo networkInfo, Wallet wallet, String address);
    Single<Token[]> fetchEnabledTokensWithBalance(Wallet wallet);
    Single<Token> saveTicker(Wallet wallet, Token token);
    Single<Token[]> saveTickers(Wallet wallet, Token[] tokens);
    Single<TokenTicker> fetchTicker(Wallet wallet, Token tokens);

    Disposable setTokenTerminated(Token token, NetworkInfo network, Wallet wallet);
    Disposable storeBlockRead(Token token, Wallet wallet);
    Single<Token[]> saveERC20Tokens(Wallet wallet, Token[] tokens);
    void deleteRealmToken(int chainId, Wallet wallet, String address);

    void updateTokenType(Token token, Wallet wallet, ContractType type);
}
