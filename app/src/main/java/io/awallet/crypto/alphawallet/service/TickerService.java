package io.awallet.crypto.alphawallet.service;

import io.awallet.crypto.alphawallet.entity.Ticker;
import io.awallet.crypto.alphawallet.entity.Token;
import io.awallet.crypto.alphawallet.entity.TokenTicker;

import io.reactivex.Observable;
import io.reactivex.Single;

public interface TickerService {

    Observable<Ticker> fetchTickerPrice(String ticker);

    Single<TokenTicker[]> fetchTokenTickers(Token[] tokens, String currency);
}
