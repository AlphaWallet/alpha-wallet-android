package com.wallet.crypto.trustapp.controller;

import com.wallet.crypto.trustapp.model.ESTransactionListResponse;
import com.wallet.crypto.trustapp.model.TRTransactionListResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by msubkhankulov on 11/30/2017.
 */

public interface TrustRayService {
    @GET("/transactions")
    Call<TRTransactionListResponse> getTransactionList(@Query("address") String address,
                                                       @Query("limit") String limit);
}
