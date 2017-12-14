package com.wallet.crypto.trustapp.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by msubkhankulov on 11/30/2017.
 */

public class TRTransactionListResponse {
    @SerializedName("docs")
    private List<TRTransaction> transactionList = new ArrayList<TRTransaction>();

    public List<TRTransaction> getTransactionList() { return transactionList; }
}
