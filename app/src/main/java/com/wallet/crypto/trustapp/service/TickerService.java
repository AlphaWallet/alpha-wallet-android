package com.wallet.crypto.trustapp.service;

import com.wallet.crypto.trustapp.entity.Ticker;

import io.reactivex.Observable;

public interface TickerService {

    Observable<Ticker> fetchTickerPrice(String ticker);
}
