package com.alphawallet.app.service;

import com.alphawallet.app.entity.NetworkInfo;
import com.alphawallet.app.entity.Ticker;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.TokenTicker;

import java.util.Map;

import io.reactivex.Single;

public interface TickerService {

    Single<Map<Integer, Ticker>> fetchTickerPrice(String ticker);
    Single<Double> convertPair(String currency1, String currency2);
    Single<TokenTicker[]> fetchTokenTickers(Token[] tokens, String currency);
    Single<Ticker> fetchEthPrice(NetworkInfo networkInfo, Ticker ticker);
    Single<Ticker> fetchBlockScoutPrice(NetworkInfo networkInfo, Ticker ticker);
}
