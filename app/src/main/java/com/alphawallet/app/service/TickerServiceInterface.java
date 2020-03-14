package com.alphawallet.app.service;

import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.tokens.Token;
import com.alphawallet.app.entity.tokens.TokenTicker;

import io.reactivex.Single;

public interface TickerServiceInterface
{
    void updateTickers(boolean forceUpdate);

    Single<Double> convertPair(String currency1, String currency2);
    Single<Token[]> getTokensOnNetwork(NetworkInfo info, String address, TokensService tokensService);
    Single<Token> attachTokenTicker(Token token);
    Single<Token[]> attachTokenTickers(Token[] tokens);
    boolean hasTickers();
    TokenTicker getTokenTicker(Token token);
    TokenTicker getEthTicker(int chainId);
    void addCustomTicker(int chainId, TokenTicker ticker);
    void addCustomTicker(String address, TokenTicker ticker);
}
