package com.wallet.crypto.trustapp.controller;

import com.wallet.crypto.trustapp.model.CMTicker;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Created by marat on 11/13/17.
 */

public interface CoinmarketService {
    @GET("/v1/ticker/ethereum")
    Call<List<CMTicker>> getEthereumPrice();
}
