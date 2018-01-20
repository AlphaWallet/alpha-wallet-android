package com.wallet.crypto.trustapp.service;

import com.wallet.crypto.trustapp.entity.Ticker;
import com.wallet.crypto.trustapp.entity.Token;
import com.wallet.crypto.trustapp.entity.TokenTicker;

import io.reactivex.Observable;
import io.reactivex.Single;

public interface TickerService {

    Observable<Ticker> fetchTickerPrice(String ticker);

    Single<TokenTicker[]> fetchTockenTickers(Token[] tokens, String currency);
}
