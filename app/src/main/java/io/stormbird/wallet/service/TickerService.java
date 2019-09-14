package io.stormbird.wallet.service;

import io.stormbird.wallet.entity.Ticker;
import io.stormbird.wallet.entity.Token;
import io.stormbird.wallet.entity.TokenTicker;

import io.reactivex.Observable;
import io.reactivex.Single;

public interface TickerService {

    Observable<Ticker> fetchTickerPrice(String ticker);
    Single<Double> convertPair(String currency1, String currency2);
    Single<TokenTicker[]> fetchTokenTickers(Token[] tokens, String currency);
}
