package com.alphawallet.app.service;

import com.alphawallet.app.entity.Ticker;
import com.alphawallet.app.entity.Token;
import com.alphawallet.app.entity.TokenTicker;

import io.reactivex.Observable;
import io.reactivex.Single;

public interface TickerService {

    Observable<Ticker> fetchTickerPrice(String ticker);
    Single<Double> convertPair(String currency1, String currency2);
    Single<TokenTicker[]> fetchTokenTickers(Token[] tokens, String currency);
}
