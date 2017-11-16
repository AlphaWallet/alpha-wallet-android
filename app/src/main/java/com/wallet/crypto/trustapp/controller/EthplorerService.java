package com.wallet.crypto.trustapp.controller;

import com.wallet.crypto.trustapp.model.EPAddressInfo;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.Path;

/**
 * Created by marat on 11/16/17.
 */

public interface EthplorerService {
    @GET("/getAddressInfo/{address}")
    Call<EPAddressInfo> getAddressInfo(@Path("address") String address, @Query("apiKey") String apiKey);
}
