package com.example.marat.wal.controller;

import com.example.marat.wal.model.ESTransactionListResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by marat on 9/26/17.
 */

public interface EtherscanService {
    @GET("/api")
    Call<ESTransactionListResponse> getTransactionList(@Query("module") String module,
                                                             @Query("action") String action,
                                                             @Query("address") String address,
                                                             @Query("startBlock") String startBlock,
                                                             @Query("sort") String sort,
                                                             @Query("apikey") String apikey);
}

