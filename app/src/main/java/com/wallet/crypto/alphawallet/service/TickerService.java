package com.wallet.crypto.alphawallet.service;

import com.wallet.crypto.alphawallet.entity.Ticker;
import com.wallet.crypto.alphawallet.entity.Token;
import com.wallet.crypto.alphawallet.entity.TokenTicker;

import io.reactivex.Observable;
import io.reactivex.Single;

public interface TickerService {

    Observable<Ticker> fetchTickerPrice(String ticker);

    Single<TokenTicker[]> fetchTokenTickers(Token[] tokens, String currency);
}
